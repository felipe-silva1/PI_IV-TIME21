const statusMessage = document.querySelector("#status-message");
const statusIndicator = document.querySelector("#status-indicator");

async function checkApi() {
    try {
        const response = await fetch("http://localhost:8080/api/health");
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