import { useState } from 'react';
import ExecutionModal from './ExecutionModal';

const StrategyCard = ({ strategy }) => {
  const [isModalOpen, setIsModalOpen] = useState(false);

  return (
    <>
      <div className="glass-card">
        <div className="card-id">ID: {strategy.id || strategy.strategyName || 'UNDEFINED'}</div>
        <h3 className="card-title">{strategy.name || strategy.id?.replace(/_/g, ' ') || 'Estrategia Genérica'}</h3>
        <button className="action-btn" onClick={() => setIsModalOpen(true)}>
          Configurar Backtest
        </button>
      </div>

      {isModalOpen && (
        <ExecutionModal 
            strategy={strategy} 
            onClose={() => setIsModalOpen(false)} 
        />
      )}
    </>
  );
};

export default StrategyCard;
