import { useState, useEffect } from 'react';
import StrategyGrid from './components/StrategyGrid';
import { fetchStrategies } from './services/apiClient';
import JobHistoryPanel from './components/JobHistoryPanel';
import PlateauVisualizer from './components/PlateauVisualizer';
import './index.css';

function App() {
  const [strategies, setStrategies] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [isHistoryOpen, setIsHistoryOpen] = useState(false);
  const [selectedJob, setSelectedJob] = useState(null);

  useEffect(() => {
    const loadData = async () => {
      try {
        const data = await fetchStrategies();
        setStrategies(data);
        setError(null);
      } catch (err) {
        setError("Imposible conectar con zEngine Backend. ¿Está Tomcat corriendo en el puerto 8080?");
      } finally {
        setLoading(false);
      }
    };

    loadData();
  }, []);

  return (
    <div className="dashboard-container">
      <header style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '40px' }}>
        <div>
          <h1>
            zEngine <span className="title-glow">Dashboard</span>
          </h1>
          <p className="subtitle">Algorithmic Trading Framework Terminal</p>
        </div>
        <button className="action-btn" onClick={() => setIsHistoryOpen(true)}>
          📜 Ver Historial Robusto
        </button>
      </header>

      <main>
        {loading && <div className="loading-spinner">Sincronizando con el Motor Java...</div>}
        {error && <div className="error-message">{error}</div>}
        {!loading && !error && <StrategyGrid strategies={strategies} />}
      </main>

      {isHistoryOpen && !selectedJob && (
        <JobHistoryPanel 
            onSelectJob={setSelectedJob} 
            onClose={() => setIsHistoryOpen(false)} 
        />
      )}

      {selectedJob && (
        <PlateauVisualizer 
            job={selectedJob} 
            onClose={() => {
                setSelectedJob(null);
                setIsHistoryOpen(true);
            }} 
        />
      )}
    </div>
  );
}

export default App;
