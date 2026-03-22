import { useState, useEffect } from 'react';
import { executeBacktest, fetchStrategyParameters } from '../services/apiClient';

const ExecutionModal = ({ strategy, onClose }) => {
  const [isExecuting, setIsExecuting] = useState(false);
  const [result, setResult] = useState(null);
  const [error, setError] = useState(null);
  
  // Dynamic parameters state
  const [parameters, setParameters] = useState([]);
  const [userRanges, setUserRanges] = useState({});
  const [isLoadingParams, setIsLoadingParams] = useState(true);

  useEffect(() => {
    const loadParams = async () => {
      try {
        const data = await fetchStrategyParameters(strategy.id);
        setParameters(data);
        
        // Inicializar el estado de los rangos con los valores por defecto del backend
        const initialRanges = {};
        data.forEach(p => {
          initialRanges[p.name] = { start: p.start, end: p.end, step: p.step };
        });
        setUserRanges(initialRanges);
      } catch (err) {
        setError("Error cargando metadatos paramétricos.");
      } finally {
        setIsLoadingParams(false);
      }
    };
    loadParams();
  }, [strategy.id]);

  const handleRangeChange = (paramName, field, value) => {
    setUserRanges(prev => ({
      ...prev,
      [paramName]: {
        ...prev[paramName],
        [field]: parseFloat(value) || 0
      }
    }));
  };

  const getCombinationsCount = () => {
    let total = 1;
    for (const key in userRanges) {
      const r = userRanges[key];
      if (r.step > 0 && r.end >= r.start) {
        const steps = Math.floor((r.end - r.start) / r.step) + 1;
        total *= steps;
      } else {
        total *= 1; // Si step es nulo o inválido cuenta como 1 combinación
      }
    }
    return total;
  };

  const handleLaunch = async () => {
    setIsExecuting(true);
    setError(null);
    try {
      // Enviamos dinámicamente el payload JSON al Orquestador
      const summary = await executeBacktest(strategy.id, userRanges);
      setResult(summary);
    } catch (err) {
      setError("El motor Java falló durante la Fuerza Bruta. Revisa los logs de Tomcat.");
    } finally {
      setIsExecuting(false);
    }
  };

  return (
    <div className="modal-overlay">
      <div className="glass-card modal-content" style={{ maxWidth: '650px' }}>
        <button className="close-btn" onClick={onClose}>&times;</button>
        <h2 className="title-glow" style={{ marginBottom: '10px' }}>Preparando {strategy.name}</h2>
        
        {isLoadingParams && (
          <div className="modal-body text-center">
            <div className="spinner" style={{ width: '30px', height: '30px', borderWidth: '3px'}}></div>
            <p className="subtitle">Extrayendo Metadata desde el Servidor...</p>
          </div>
        )}

        {!result && !isExecuting && !isLoadingParams && (
          <div className="modal-body">
            <p className="subtitle" style={{ fontSize: '0.95rem', marginBottom: '20px' }}>
              Configura los límites para el barrido matemático. El motor generará dinámicamente combinaciones utilizando producto cartesiano sin bucles finitos.
            </p>

            <div className="params-container">
              {parameters.map((p) => (
                <div key={p.name} className="param-row">
                  <div className="param-name">{p.name}</div>
                  <div className="param-inputs">
                    <label>
                      Min
                      <input type="number" step="any" 
                        value={userRanges[p.name]?.start ?? ''}
                        onChange={(e) => handleRangeChange(p.name, 'start', e.target.value)} />
                    </label>
                    <label>
                      Max
                      <input type="number" step="any" 
                        value={userRanges[p.name]?.end ?? ''}
                        onChange={(e) => handleRangeChange(p.name, 'end', e.target.value)} />
                    </label>
                    <label>
                      Step
                      <input type="number" step="any" 
                        value={userRanges[p.name]?.step ?? ''}
                        onChange={(e) => handleRangeChange(p.name, 'step', e.target.value)} />
                    </label>
                  </div>
                </div>
              ))}
            </div>
            
            <div className="preview-badge" style={{ marginTop: '20px', textAlign: 'center' }}>
              <span className="metric-label">Proyección de Fuerza Bruta: </span>
              <strong className="job-id" style={{ fontSize: '1.2rem'}}>{getCombinationsCount().toLocaleString()}</strong>
              <span className="subtitle" style={{marginLeft: '8px'}}>Combinaciones</span>
            </div>

            <br/>
            <button className="action-btn launch-btn" onClick={handleLaunch}>🚀 Iniciar Motor Fuerza Bruta</button>
          </div>
        )}

        {isExecuting && (
          <div className="modal-body text-center" style={{ padding: '40px 0'}}>
            <div className="spinner"></div>
            <p className="subtitle animate-pulse" style={{marginTop: '20px'}}>
              Calculando {getCombinationsCount().toLocaleString()} iteraciones en el Back-end...<br/>
              (Tomcat procesando, por favor espera)
            </p>
          </div>
        )}

        {result && (
          <div className="modal-body success-state">
            <div className="success-icon">✅</div>
            <h3 className="text-center">¡Fuerza Bruta Finalizada!</h3>
            <div className="metrics-grid">
              <div className="metric-box">
                <span className="metric-label">Job ID</span>
                <span className="metric-value job-id">{result.jobId.split('-')[0]}...</span>
              </div>
              <div className="metric-box">
                <span className="metric-label">Evaluadas</span>
                <span className="metric-value">{result.totalCombinationsEvaluated}</span>
              </div>
              <div className="metric-box">
                <span className="metric-label">Tiempo</span>
                <span className="metric-value">{result.durationSeconds}s</span>
              </div>
            </div>
            <p className="subtitle text-sm text-center" style={{marginTop: '20px'}}>
              Resultados exportados nativamente a tu base PostgreSQL.
            </p>
            <button className="action-btn" style={{marginTop: '20px', width: '100%'}} onClick={onClose}>
              Cerrar Tablero
            </button>
          </div>
        )}

        {error && (
          <div className="error-message" style={{marginTop: '20px'}}>
            {error}
          </div>
        )}
      </div>
    </div>
  );
};

export default ExecutionModal;
