const API_BASE_URL = 'http://localhost:8080/api';

export const fetchStrategies = async () => {
    try {
        const response = await fetch(`${API_BASE_URL}/strategies`, {
            method: 'GET',
            headers: {
                'Accept': 'application/json',
            }
        });
        
        if (!response.ok) {
            throw new Error(`Error en el servidor: ${response.status}`);
        }
        
        return await response.json();
    } catch (error) {
        console.error("Hubo un error contactando al zEngine API:", error);
        throw error;
    }
};

export const executeBacktest = async (strategyId, rangesPayload) => {
    try {
        const response = await fetch(`${API_BASE_URL}/strategies/${encodeURIComponent(strategyId)}/execute`, {
            method: 'POST',
            headers: {
                'Accept': 'application/json',
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(rangesPayload)
        });
        
        if (!response.ok) {
            throw new Error(`Error en el servidor al ejecutar: ${response.status}`);
        }
        
        return await response.json();
    } catch (error) {
        console.error("Fallo la ejecución de la estrategia:", error);
        throw error;
    }
};

export const fetchStrategyParameters = async (strategyId) => {
    try {
        const response = await fetch(`${API_BASE_URL}/strategies/${encodeURIComponent(strategyId)}/parameters`);
        if (!response.ok) {
            throw new Error(`Error HTTP: ${response.status}`);
        }
        return await response.json();
    } catch (error) {
        console.error("Error obteniendo parámetros del API:", error);
        throw error;
    }
};

export const fetchJobs = async () => {
    try {
        const response = await fetch(`${API_BASE_URL}/jobs`);
        if (!response.ok) throw new Error(`Error HTTP: ${response.status}`);
        return await response.json();
    } catch (error) {
        console.error("Error obteniendo el historial de Jobs:", error);
        throw error;
    }
};

export const fetchJobResults = async (jobId) => {
    try {
        const response = await fetch(`${API_BASE_URL}/jobs/${jobId}/results`);
        if (!response.ok) throw new Error(`Error HTTP: ${response.status}`);
        return await response.json();
    } catch (error) {
        console.error("Error obteniendo los resultados del Job:", error);
        throw error;
    }
};
