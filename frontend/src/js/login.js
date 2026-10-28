const formLogin = document.getElementById('form-login');
const emailValido = v => /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(v);

function marcar(campos, ruim) {
  campos.forEach(c => c.setAttribute('aria-invalid', c === ruim ? 'true' : 'false'));
}

document.querySelector('.mostrar-senha').addEventListener('click', e => {
  const btn = e.currentTarget;
  const senha = btn.previousElementSibling;
  const mostrar = senha.type === 'password';
  senha.type = mostrar ? 'text' : 'password';
  btn.textContent = mostrar ? 'Ocultar' : 'Mostrar';
  btn.setAttribute('aria-label', mostrar ? 'Ocultar senha' : 'Mostrar senha');
});

formLogin.addEventListener('submit', e => {
  e.preventDefault();
  const [email, senha] = formLogin.querySelectorAll('input');
  const erro = document.getElementById('l-erro');
  const ok = document.getElementById('l-ok');
  ok.textContent = '';
  let msg = '', ruim = null;

  if (!emailValido(email.value)) { msg = 'Digite um e-mail válido.'; ruim = email; }
  else if (!senha.value) { msg = 'Digite sua senha.'; ruim = senha; }

  marcar([email, senha], ruim);
  erro.textContent = msg;
  if (ruim) return ruim.focus();

  ok.textContent = 'Login válido! Redirecionando…';
  location.href = 'pages/dashboard.html';
});
