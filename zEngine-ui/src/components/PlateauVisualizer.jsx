import { useState, useEffect, useMemo, useCallback } from 'react';
import { fetchJobResults } from '../services/apiClient';

const PlateauVisualizer = ({ job, onClose }) => {
    const [results, setResults] = useState([]);
    const [loading, setLoading] = useState(true);
    const [xAxis, setXAxis] = useState('');
    const [yAxis, setYAxis] = useState('');
    const [availableParams, setAvailableParams] = useState([]);
    const [hoveredCell, setHoveredCell] = useState(null);
    const [visualHighlight, setVisualHighlight] = useState('None');
    const [fixedParams, setFixedParams] = useState({});

    useEffect(() => {
        fetchJobResults(job.id).then(data => {
            setResults(data);
            if (data.length > 0) {
                try {
                    const validRow = data.find(r => r.parametersJson != null);
                    if (validRow) {
                        const firstParams = JSON.parse(validRow.parametersJson);
                        const keys = Object.keys(firstParams).sort();
                        setAvailableParams(keys);
                        
                        if (keys.length >= 2) {
                            setXAxis(keys[0]);
                            setYAxis(keys[1]);
                        } else if (keys.length === 1) {
                            setXAxis(keys[0]);
                            setYAxis(keys[0]);
                        }
                    }
                } catch (e) {
                    console.warn("Fallo al deducir los ejes paramétricos:", e);
                }
            }
            setLoading(false);
        }).catch(err => {
            console.error("Error al graficar Job Results:", err);
            setLoading(false);
        });
    }, [job.id]);

    // ──────────────────────────────────────────────────────────
    // DIMENSION SLICING: extra param detection + unique values
    // ──────────────────────────────────────────────────────────
    const extraParamsData = useMemo(() => {
        if (results.length === 0 || !xAxis || !yAxis) return {};

        // Detect all params NOT on X or Y
        const extraKeys = availableParams.filter(k => k !== xAxis && k !== yAxis);
        const data = {};

        extraKeys.forEach(key => {
            const uniqueVals = new Set();
            results.forEach(r => {
                try {
                    if (!r.parametersJson) return;
                    const params = JSON.parse(r.parametersJson);
                    const raw = params[key];
                    if (raw !== undefined) {
                        uniqueVals.add(isNaN(Number(raw)) ? raw : Number(raw));
                    }
                } catch (e) { /* skip */ }
            });
            
            const sorted = [...uniqueVals].sort((a, b) => {
                if (typeof a === 'number' && typeof b === 'number') return a - b;
                return String(a).localeCompare(String(b));
            });
            
            if (sorted.length > 0) {
                data[key] = sorted;
            }
        });

        return data;
    }, [results, xAxis, yAxis, availableParams]);

    // Auto-initialize fixedParams when extraParamsData changes
    useEffect(() => {
        const newFixed = {};
        Object.entries(extraParamsData).forEach(([key, values]) => {
            // Preserve current selection if it's still valid, otherwise reset to first
            if (fixedParams[key] !== undefined && values.includes(fixedParams[key])) {
                newFixed[key] = fixedParams[key];
            } else {
                newFixed[key] = values[0];
            }
        });
        setFixedParams(newFixed);
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [extraParamsData]);

    // Slider change handler
    const handleSliderChange = useCallback((paramKey, sliderIndex) => {
        const values = extraParamsData[paramKey];
        if (values) {
            setFixedParams(prev => ({ ...prev, [paramKey]: values[sliderIndex] }));
        }
    }, [extraParamsData]);

    // ──────────────────────────────────────────────────────────
    // HEATMAP MATRIX with Dimension Slice Filtering
    // ──────────────────────────────────────────────────────────
    const { matrix, xValues, yValues, minProfit, maxProfit, sliceCount, totalCount } = useMemo(() => {
        if (results.length === 0 || !xAxis || !yAxis) return { matrix: [], xValues: [], yValues: [], minProfit: 0, maxProfit: 0, sliceCount: 0, totalCount: 0 };

        // STEP 1: Parse all results
        const allParsed = [];
        results.forEach(r => {
            try {
                if (!r.parametersJson) return;
                const params = JSON.parse(r.parametersJson);
                allParsed.push({ params, result: r });
            } catch (e) { /* skip */ }
        });

        // STEP 2: Apply dimension slice filter
        const filtered = allParsed.filter(({ params }) => {
            return Object.entries(fixedParams).every(([key, pinnedVal]) => {
                const raw = params[key];
                if (raw === undefined) return true; // If param missing, don't exclude
                const val = isNaN(Number(raw)) ? raw : Number(raw);
                return val === pinnedVal;
            });
        });

        // STEP 3: Build parsed result array from filtered data
        const parsedResults = [];
        let minP = 0;
        let maxP = 0;

        filtered.forEach(({ params, result: r }) => {
            let rawX = params[xAxis];
            let rawY = params[yAxis];
            if (rawX === undefined || rawY === undefined) return;

            const xVal = isNaN(Number(rawX)) ? rawX : Number(rawX);
            const yVal = isNaN(Number(rawY)) ? rawY : Number(rawY);
            const profit = Number(r.netProfitUsd) || 0;

            if (profit < minP) minP = profit;
            if (profit > maxP) maxP = profit;

            parsedResults.push({
                x: xVal,
                y: yVal,
                profit,
                winRate: Number(r.winRatePct) || 0,
                trades: r.totalTrades,
                drawdown: Number(r.maxDrawdown) || 0
            });
        });

        // STEP 4: Build matrix
        const uniqueX = [...new Set(parsedResults.map(r => r.x))].sort((a, b) => {
            if (typeof a === 'number' && typeof b === 'number') return a - b;
            return String(a).localeCompare(String(b));
        });
        
        const uniqueY = [...new Set(parsedResults.map(r => r.y))].sort((a, b) => {
            if (typeof a === 'number' && typeof b === 'number') return b - a;
            return String(b).localeCompare(String(a));
        });

        const mat = [];
        uniqueY.forEach(y => {
            const row = [];
            uniqueX.forEach(x => {
                const cellData = parsedResults.find(r => r.x === x && r.y === y);
                row.push(cellData || null);
            });
            mat.push(row);
        });

        return { matrix: mat, xValues: uniqueX, yValues: uniqueY, minProfit: minP, maxProfit: maxP, sliceCount: filtered.length, totalCount: allParsed.length };
    }, [results, xAxis, yAxis, fixedParams]);

    // ──────────────────────────────────────────────────────────
    // COLOR SCALE
    // ──────────────────────────────────────────────────────────
    const getCellColor = (profit) => {
        if (profit == null) return 'transparent';
        
        if (profit > 0) {
            const ratio = maxProfit > 0 ? Math.min(profit / maxProfit, 1) : 0;
            return `hsl(130, ${40 + (ratio * 40)}%, ${30 + (ratio * 25)}%)`;
        } else if (profit < 0) {
            const ratio = minProfit < 0 ? Math.min(profit / minProfit, 1) : 0;
            return `hsl(350, ${50 + (ratio * 40)}%, ${45 - (ratio * 10)}%)`;
        } else {
            return `hsl(220, 15%, 25%)`;
        }
    };

    // Responsive scaling
    const MIN_CELL_WIDTH = 45;
    const MIN_CELL_HEIGHT = 30;
    const gridWidth = Math.max(600, xValues.length * MIN_CELL_WIDTH);
    const gridHeight = Math.max(300, yValues.length * MIN_CELL_HEIGHT);
    const hasExtraParams = Object.keys(extraParamsData).length > 0;

    return (
        <div className="modal-overlay">
            <div className="glass-card modal-content" style={{maxWidth: '1200px', width: '95vw', height: '90vh', display: 'flex', flexDirection: 'column', padding: '25px', backgroundColor: '#0f111a'}}>
                
                {/* Header */}
                <div style={{display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '15px', flexShrink: 0}}>
                    <h2 style={{margin: 0, fontSize: '1.2rem', fontWeight: 600, color: '#e2e8f0'}}>
                        Grid Search Heatmap: <span style={{color: '#00ff88'}}>{job.strategyName}</span>
                    </h2>
                    
                    <div style={{display: 'flex', gap: '20px', border: '1px solid rgba(255,255,255,0.1)', padding: '5px 20px', borderRadius: '8px', fontSize: '0.85rem', color: '#a0aabf'}}>
                        <div style={{display: 'flex', flexDirection: 'column'}}>
                            <span style={{fontSize: '0.65rem', textTransform: 'uppercase', opacity: 0.7}}>Optimization</span>
                            <strong style={{color: '#fff'}}>Grid Search</strong>
                        </div>
                        <div style={{display: 'flex', flexDirection: 'column'}}>
                            <span style={{fontSize: '0.65rem', textTransform: 'uppercase', opacity: 0.7}}>Parameters</span>
                            <strong style={{color: '#fff'}}>{results.length} Combinations</strong>
                        </div>
                        {hasExtraParams && (
                            <div style={{display: 'flex', flexDirection: 'column'}}>
                                <span style={{fontSize: '0.65rem', textTransform: 'uppercase', opacity: 0.7}}>Slice</span>
                                <strong style={{color: '#c084fc'}}>{sliceCount} of {totalCount}</strong>
                            </div>
                        )}
                        <div style={{display: 'flex', flexDirection: 'column'}}>
                            <span style={{fontSize: '0.65rem', textTransform: 'uppercase', opacity: 0.7}}>Status</span>
                            <strong style={{color: '#00ff88'}}>Ready</strong>
                        </div>
                        <button className="close-btn" style={{position: 'static', padding: '0 0 0 15px', borderLeft: '1px solid rgba(255,255,255,0.1)', marginLeft: '10px'}} onClick={onClose}>&times;</button>
                    </div>
                </div>

                {/* Axis Dropdowns */}
                <div className="params-container" style={{display: 'flex', flexDirection: 'row', gap: '40px', padding: '0 0 15px 0', borderBottom: '1px solid rgba(255,255,255,0.05)', flexShrink: 0}}>
                    <label className="param-name" style={{display:'flex', alignItems:'center', gap: '15px', color: '#00ff88'}}>
                        Y-Axis (Rows)
                        <select className="glass-select" value={yAxis} onChange={e => setYAxis(e.target.value)}>
                            {availableParams.map(p => <option key={p} value={p}>{p}</option>)}
                        </select>
                    </label>

                    <label className="param-name" style={{display:'flex', alignItems:'center', gap: '15px', color: '#00ff88'}}>
                        X-Axis (Columns)
                        <select className="glass-select" value={xAxis} onChange={e => setXAxis(e.target.value)}>
                            {availableParams.map(p => <option key={p} value={p}>{p}</option>)}
                        </select>
                    </label>
                </div>

                {/* ─── DIMENSION SLICING PANEL ─── */}
                {hasExtraParams && !loading && (
                    <div className="slice-panel" style={{
                        display: 'flex',
                        flexDirection: 'row',
                        flexWrap: 'wrap',
                        gap: '20px',
                        padding: '12px 15px',
                        marginTop: '10px',
                        background: 'hsla(270, 30%, 15%, 0.25)',
                        border: '1px solid hsla(270, 70%, 50%, 0.2)',
                        borderRadius: '10px',
                        flexShrink: 0
                    }}>
                        <div style={{display: 'flex', alignItems: 'center', gap: '8px', marginRight: '10px'}}>
                            <span style={{fontSize: '0.75rem', textTransform: 'uppercase', color: '#c084fc', fontWeight: 700, letterSpacing: '0.5px'}}>
                                Dimension Slice
                            </span>
                        </div>
                        
                        {Object.entries(extraParamsData).map(([paramKey, values]) => {
                            const currentVal = fixedParams[paramKey];
                            const currentIdx = values.indexOf(currentVal);
                            
                            return (
                                <div key={paramKey} style={{display: 'flex', alignItems: 'center', gap: '12px', flex: '1 1 200px', minWidth: '200px'}}>
                                    <span className="param-name" style={{fontSize: '0.85rem', minWidth: 'fit-content', color: '#00ff88'}}>
                                        {paramKey}
                                    </span>
                                    
                                    <input
                                        type="range"
                                        className="dimension-slider"
                                        min={0}
                                        max={values.length - 1}
                                        value={currentIdx >= 0 ? currentIdx : 0}
                                        onChange={e => handleSliderChange(paramKey, parseInt(e.target.value))}
                                        style={{flex: 1}}
                                    />
                                    
                                    <span style={{
                                        background: 'hsla(270, 70%, 50%, 0.25)',
                                        border: '1px solid hsla(270, 70%, 50%, 0.4)',
                                        padding: '3px 10px',
                                        borderRadius: '6px',
                                        fontFamily: "'Fira Code', monospace",
                                        fontSize: '0.85rem',
                                        color: '#fff',
                                        fontWeight: 600,
                                        minWidth: '45px',
                                        textAlign: 'center'
                                    }}>
                                        {currentVal}
                                    </span>
                                </div>
                            );
                        })}
                    </div>
                )}
                
                {loading ? (
                    <div className="text-center" style={{padding: '100px', flex: 1}}>
                        <div className="spinner" style={{width: '60px', height: '60px', borderWidth: '4px'}}></div>
                        <p className="subtitle animate-pulse" style={{marginTop: '20px'}}>Renderizando DOM Celular Nativo...</p>
                    </div>
                ) : results.length === 0 ? (
                    <p className="text-center subtitle" style={{flex: 1, paddingTop: '100px'}}>No records found inside this Job.</p>
                ) : (xValues.length > 0 && yValues.length > 0) ? (
                    <div style={{display: 'flex', flexDirection: 'column', flex: 1, minHeight: 0}}>
                        
                        {/* Heatmap Grid with Overflow Scroll */}
                        <div style={{flex: 1, display: 'flex', flexDirection: 'row', position: 'relative', marginTop: '15px', overflow: 'auto', background: 'hsla(220, 20%, 6%, 0.5)', border: '1px solid rgba(255,255,255,0.05)', borderRadius: '12px'}}>
                            
                            <div style={{display: 'flex', flexDirection: 'row', minWidth: `${gridWidth}px`, minHeight: `${gridHeight}px`, padding: '20px' }}>
                                
                                {/* Y-Axis Label + Values */}
                                <div style={{display: 'flex', flexDirection: 'row', alignItems: 'stretch', paddingRight: '15px'}}>
                                    <div style={{display: 'flex', alignItems: 'center', justifyContent: 'center', width: '30px'}}>
                                        <div style={{transform: 'rotate(-90deg)', whiteSpace: 'nowrap', color: '#8b949e', fontSize: '0.9rem', fontWeight: 500}}>
                                            {yAxis}
                                        </div>
                                    </div>
                                    <div style={{display: 'flex', flexDirection: 'column', justifyContent: 'space-between', flex: 1}}>
                                        {yValues.map(y => (
                                            <div key={`y-${y}`} style={{color: '#8b949e', fontSize: '0.8rem', textAlign: 'right', display: 'flex', alignItems: 'center', justifyContent: 'flex-end', height: '100%', paddingRight: '5px'}}>
                                                {y}
                                            </div>
                                        ))}
                                    </div>
                                </div>

                                {/* Heatmap Cells */}
                                <div style={{flex: 1, display: 'flex', flexDirection: 'column', position: 'relative'}}>
                                    <div style={{flex: 1, display: 'flex', flexDirection: 'column', border: '1px solid rgba(255,255,255,0.1)'}}>
                                        {matrix.map((row, rIdx) => (
                                            <div key={`row-${rIdx}`} style={{display: 'flex', flex: 1, borderBottom: rIdx < matrix.length - 1 ? '1px solid rgba(0,0,0,0.5)' : 'none'}}>
                                                {row.map((cell, cIdx) => {
                                                    const isHovered = hoveredCell === cell;
                                                    const isHighlighted = visualHighlight !== 'None' && cell && 
                                                        ((visualHighlight === 'Profitable' && cell.profit > 0) || 
                                                         (visualHighlight === 'Losses' && cell.profit < 0));

                                                    let opacity = 1;
                                                    if (visualHighlight !== 'None' && !isHighlighted) opacity = 0.15;
                                                    
                                                    return (
                                                        <div 
                                                            key={`cell-${rIdx}-${cIdx}`}
                                                            onMouseEnter={() => cell && setHoveredCell(cell)}
                                                            onMouseLeave={() => setHoveredCell(null)}
                                                            style={{
                                                                flex: 1, 
                                                                minWidth: `${MIN_CELL_WIDTH}px`,
                                                                backgroundColor: cell ? getCellColor(cell.profit) : 'rgba(255,255,255,0.02)',
                                                                borderRight: cIdx < row.length - 1 ? '1px solid rgba(0,0,0,0.5)' : 'none',
                                                                transition: 'all 0.15s ease',
                                                                opacity: opacity,
                                                                cursor: cell ? 'crosshair' : 'default',
                                                                boxShadow: isHovered ? 'inset 0 0 0 2px #fff' : 'none',
                                                                zIndex: isHovered ? 10 : 1
                                                            }}
                                                        />
                                                    );
                                                })}
                                            </div>
                                        ))}
                                    </div>

                                    {/* X-Axis Labels */}
                                    <div style={{display: 'flex', marginTop: '10px', height: '30px'}}>
                                        {xValues.map((x, idx) => {
                                            const showLabel = xValues.length <= 20 || idx % Math.ceil(xValues.length / 15) === 0 || idx === xValues.length - 1;
                                            return (
                                                <div key={`x-${x}`} style={{flex: 1, color: '#8b949e', fontSize: '0.8rem', textAlign: 'center', opacity: showLabel ? 1 : 0}}>
                                                    {x}
                                                </div>
                                            );
                                        })}
                                    </div>
                                    <div style={{textAlign: 'center', color: '#8b949e', fontSize: '0.9rem', fontWeight: 500, marginTop: '5px'}}>
                                        {xAxis}
                                    </div>
                                </div>

                                {/* Color Bar */}
                                <div style={{width: '60px', display: 'flex', flexDirection: 'column', alignItems: 'center', marginLeft: '30px', paddingBottom: '35px'}}>
                                    <div style={{color: '#00ff88', fontSize: '0.75rem', marginBottom: '5px', fontWeight: 'bold'}}>
                                        +${maxProfit.toFixed(0)}
                                    </div>
                                    <div style={{
                                        flex: 1,
                                        width: '12px',
                                        background: 'linear-gradient(to bottom, hsl(130, 60%, 50%), hsl(220, 15%, 25%), hsl(350, 70%, 45%))',
                                        borderRadius: '6px',
                                        boxShadow: 'inset 0 0 5px rgba(0,0,0,0.5)'
                                    }}></div>
                                    <div style={{color: '#ff4d4d', fontSize: '0.75rem', marginTop: '5px', fontWeight: 'bold'}}>
                                        -${Math.abs(minProfit).toFixed(0)}
                                    </div>
                                </div>
                            </div>

                            {/* Hover Tooltip */}
                            {hoveredCell && (
                                <div style={{
                                    position: 'sticky',
                                    bottom: '20px',
                                    left: 'calc(100% - 250px)',
                                    marginTop: 'auto',
                                    backgroundColor: 'rgba(15, 17, 26, 0.95)',
                                    border: '1px solid rgba(255,255,255,0.1)',
                                    borderRadius: '8px',
                                    padding: '15px',
                                    boxShadow: '0 10px 30px rgba(0,0,0,0.5)',
                                    zIndex: 100,
                                    minWidth: '220px',
                                    backdropFilter: 'blur(10px)',
                                    pointerEvents: 'none'
                                }}>
                                    <div style={{fontSize: '0.75rem', color: '#8b949e', textTransform: 'uppercase', marginBottom: '5px'}}>Cell Metrics</div>
                                    <div style={{fontSize: '1.2rem', fontWeight: 'bold', color: hoveredCell.profit > 0 ? '#00ff88' : '#ff4d4d', marginBottom: '10px'}}>
                                        {hoveredCell.profit > 0 ? '+' : ''}${hoveredCell.profit.toFixed(2)}
                                    </div>
                                    <div style={{display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '8px', fontSize: '0.85rem'}}>
                                        <div style={{color: '#8b949e'}}>X ({xAxis}):</div><div style={{color: '#fff', textAlign: 'right'}}>{hoveredCell.x}</div>
                                        <div style={{color: '#8b949e'}}>Y ({yAxis}):</div><div style={{color: '#fff', textAlign: 'right'}}>{hoveredCell.y}</div>
                                        <div style={{color: '#8b949e'}}>Win Rate:</div><div style={{color: '#fff', textAlign: 'right'}}>{hoveredCell.winRate}%</div>
                                        <div style={{color: '#8b949e'}}>Drawdown:</div><div style={{color: '#fff', textAlign: 'right'}}>{hoveredCell.drawdown}%</div>
                                        <div style={{color: '#8b949e'}}>Trades:</div><div style={{color: '#fff', textAlign: 'right'}}>{hoveredCell.trades}</div>
                                    </div>
                                </div>
                            )}

                        </div>

                        {/* Footer Controls */}
                        <div style={{display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginTop: '15px', paddingTop: '15px', borderTop: '1px solid rgba(255,255,255,0.05)', flexShrink: 0}}>
                            <label style={{display: 'flex', alignItems: 'center', gap: '15px', color: '#fff', fontSize: '0.9rem', fontWeight: 500}}>
                                Visual Highlight
                                <select 
                                    className="glass-select" 
                                    style={{minWidth: '150px'}}
                                    value={visualHighlight} 
                                    onChange={e => setVisualHighlight(e.target.value)}
                                >
                                    <option value="None">None</option>
                                    <option value="Profitable">Profitable Only</option>
                                    <option value="Losses">Losses Only</option>
                                </select>
                            </label>

                            <button 
                                className="action-btn" 
                                style={{background: 'transparent', border: '1px solid rgba(255,255,255,0.2)'}}
                                onClick={() => { setVisualHighlight('None'); setHoveredCell(null); }}
                            >
                                Reset View
                            </button>
                        </div>
                    </div>
                ) : (
                    <div className="text-center" style={{flex: 1, paddingTop: '80px'}}>
                        <p className="subtitle">No cells matched the current slice. Try adjusting the dimension sliders.</p>
                    </div>
                )}
            </div>
        </div>
    );
};

export default PlateauVisualizer;
