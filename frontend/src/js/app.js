const API_BASE_URL = "http://localhost:8080";
const API_HEALTH_URL = `${API_BASE_URL}/api/health`;

const statusMessage = document.querySelector("#status-message");
const statusIndicator = document.querySelector("#status-indicator");

async function checkApi() {
    try {
        const response = await fetch(API_HEALTH_URL);
        if (!response.ok) {
            throw new Error("A API respondeu com erro.");
        }

        const data = await response.json();
        statusMessage.textContent = data.message;
        statusIndicator.dataset.state = "online";
    } catch {
        statusMessage.textContent = "API indisponível. Inicie o backend Java para conectar.";
        statusIndicator.dataset.state = "offline";
    }
}

checkApi();