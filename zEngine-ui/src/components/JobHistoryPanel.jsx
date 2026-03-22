import { useEffect, useState } from 'react';
import { fetchJobs } from '../services/apiClient';

const JobHistoryPanel = ({ onSelectJob, onClose }) => {
    const [jobs, setJobs] = useState([]);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        fetchJobs().then(data => {
            // Sort jobs by start time descending (newest first)
            setJobs(data.sort((a,b) => new Date(b.startTime) - new Date(a.startTime)));
            setLoading(false);
        }).catch(err => {
            console.error(err);
            setLoading(false);
        });
    }, []);

    return (
        <div className="modal-overlay">
            <div className="glass-card modal-content" style={{maxWidth: '800px'}}>
                <button className="close-btn" onClick={onClose}>&times;</button>
                <h2 className="title-glow text-center" style={{marginBottom: '20px'}}>Historial de Optimizaciones</h2>
                
                {loading ? (
                    <div className="text-center" style={{padding: '40px'}}><div className="spinner"></div></div>
                ) : (
                    <div className="jobs-list" style={{ marginTop: '20px', maxHeight: '60vh', overflowY: 'auto', paddingRight: '10px' }}>
                        {jobs.length === 0 ? <p className="text-center subtitle">No hay trabajos registrados.</p> : null}
                        
                        {jobs.map(job => (
                            <div key={job.id} 
                                 className="param-row" 
                                 style={{ cursor: 'pointer', padding: '15px', borderRadius: '8px', transition: 'all 0.2s', marginBottom: '10px' }} 
                                 onClick={() => onSelectJob(job)}
                                 onMouseOver={(e) => e.currentTarget.style.background = 'hsla(270, 70%, 50%, 0.15)'}
                                 onMouseOut={(e) => e.currentTarget.style.background = 'transparent'}
                            >
                                <div style={{flex: 1}}>
                                    <span className="param-name" style={{fontSize: '1.1rem'}}>{job.strategyName}</span>
                                    <div className="subtitle text-sm" style={{marginTop: '4px'}}>Inicio: {new Date(job.startTime).toLocaleString()}</div>
                                </div>
                                <div style={{textAlign: 'right'}}>
                                    <span className="metric-label" style={{
                                        color: job.status === 'COMPLETED' ? 'var(--accent-emerald)' : 
                                               job.status === 'FAILED' ? '#ff4d4d' : 'var(--accent-blue)',
                                        fontWeight: 'bold'
                                    }}>
                                        {job.status}
                                    </span>
                                    <div className="job-id text-sm" style={{marginTop: '4px'}}>{job.combinationsCount.toLocaleString()} combinaciones</div>
                                </div>
                            </div>
                        ))}
                    </div>
                )}
            </div>
        </div>
    );
};

export default JobHistoryPanel;
