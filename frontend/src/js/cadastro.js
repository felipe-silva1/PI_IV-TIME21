const formCadastro = document.getElementById('form-cadastro');
const emailValido = v => /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(v);

function marcar(campos, ruim) {
  campos.forEach(c => c.setAttribute('aria-invalid', c === ruim ? 'true' : 'false'));
}


formCadastro.addEventListener('submit', e => {
  e.preventDefault();
  const [nome, email, senha, conf] = formCadastro.querySelectorAll('input');
  const erro = document.getElementById('c-erro');
  const ok = document.getElementById('c-ok');
  ok.textContent = '';
  let msg = '', ruim = null;

  if (nome.value.trim().length < 2) { msg = 'Informe seu nome completo.'; ruim = nome; }
  else if (!emailValido(email.value)) { msg = 'Digite um e-mail válido.'; ruim = email; }
  else if (senha.value.length < 8) { msg = 'A senha precisa ter pelo menos 8 caracteres.'; ruim = senha; }
  else if (senha.value !== conf.value) { msg = 'As senhas não são iguais.'; ruim = conf; }

  marcar([nome, email, senha, conf], ruim);
  erro.textContent = msg;
  if (ruim) return ruim.focus();

  
  ok.textContent = 'Conta criada! Redirecionando…';
  location.href = 'escolha-exame.html';
});
