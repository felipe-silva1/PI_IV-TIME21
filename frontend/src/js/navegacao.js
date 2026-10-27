// Botões com data-pagina="destino.html" levam para a página indicada
document.querySelectorAll('[data-pagina]').forEach(botao => {
  botao.addEventListener('click', () => {
    location.href = botao.dataset.pagina;
  });
});
