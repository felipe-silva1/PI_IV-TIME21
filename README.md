# PI IV - TIME21

Estrutura inicial para o projeto web, com frontend em HTML, CSS e JavaScript e backend em Java puro. O backend usa o servidor HTTP incluído na JDK; não usa Spring Boot.

## Estrutura

```text
PI_IV-TIME21/
|-- backend/
|   |-- pom.xml
|   `-- src/main/java/br/edu/time21/
|       |-- Application.java
|       |-- config/ApiServer.java
|       `-- controller/HealthController.java
|-- frontend/
|   |-- index.html
|   `-- src/
|       |-- css/styles.css
|       `-- js/app.js
|-- .gitignore
`-- README.md
```

## Requisitos

- JDK 17 ou superior
- Maven 3.8 ou superior
- VS Code com a extensão Live Server (para servir o frontend durante o desenvolvimento)

## Executar

1. Inicie a API a partir da pasta `backend`:

	```powershell
	mvn clean package
	java -jar target/time21-backend-1.0.0.jar
	```

	A API ficará disponível em `http://localhost:8080`; verifique `http://localhost:8080/api/health`.

2. Abra `frontend/index.html` com o Live Server. A página consulta o endpoint de saúde da API.

O CORS do servidor permite as origens locais padrão do Live Server (`localhost` e `127.0.0.1`, porta `5500`). Se a extensão usar outra porta, ajuste a lista em `backend/src/main/java/br/edu/time21/config/ApiServer.java`.

## Próximos módulos

À medida que os requisitos do sistema forem definidos, mantenha as responsabilidades separadas: endpoints em `controller`, regras de negócio em `service`, acesso a dados em `repository` e entidades/DTOs em `model`. No frontend, mantenha estrutura, estilos e comportamento em seus respectivos arquivos; módulos adicionais podem ser separados em `src` conforme crescerem.