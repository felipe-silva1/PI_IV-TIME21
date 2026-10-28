const API_BASE_URL = "http://localhost:8080";
const API_HEALTH_URL = `${API_BASE_URL}/api/health`;

const statusMessage = document.querySelector("#status-message");
const statusIndicator = document.querySelector("#status-indicator");
const serverStatus = document.querySelector("#server-status");
const databaseStatus = document.querySelector("#database-status");

async function checkApi() {
    try {
        const response = await fetch(API_HEALTH_URL, { signal: AbortSignal.timeout(6000) });
        if (!response.ok) {
            throw new Error("A API respondeu com erro.");
        }

        const data = await response.json();
        const online = data.status === "UP";
        const serverOnline = data.servidorDisponivel === true;
        const databaseOnline = data.bancoDisponivel === true;

        statusMessage.textContent = data.mensagem || "Status recebido.";
        serverStatus.textContent = serverOnline ? "Disponível" : "Indisponível";
        databaseStatus.textContent = databaseOnline ? "Disponível" : "Indisponível";
        statusIndicator.dataset.state = online ? "online" : "offline";
    } catch (error) {
        statusMessage.textContent = "Sistema indisponível. Inicie o backend e o servidor Java.";
        serverStatus.textContent = "Indisponível";
        databaseStatus.textContent = "Indisponível";
        statusIndicator.dataset.state = "offline";
    }
}

checkApi();