// No React import needed
import StrategyCard from './StrategyCard';

const StrategyGrid = ({ strategies }) => {
  if (!strategies || strategies.length === 0) {
    return (
      <div className="loading-spinner">
        No se encontraron estrategias dinámicas en el servidor.
      </div>
    );
  }

  return (
    <div className="glass-grid">
      {strategies.map((strat, index) => (
        <StrategyCard key={strat.id || index} strategy={strat} />
      ))}
    </div>
  );
};

export default StrategyGrid;
