document.addEventListener('DOMContentLoaded', () => {
  const loginForm = document.getElementById('loginForm');
  const registerForm = document.getElementById('registerForm');
  const loginMessage = document.getElementById('loginMessage');
  const registerMessage = document.getElementById('registerMessage');
  const toggleButtons = document.querySelectorAll('.toggle-password');

  if (loginForm) {
    loginForm.addEventListener('submit', (event) => {
      event.preventDefault();

      const email = document.getElementById('loginEmail').value.trim();
      const password = document.getElementById('loginPassword').value.trim();

      if (!email || !password) {
        loginMessage.textContent = 'Preencha e-mail e senha para continuar.';
        loginMessage.className = 'form-message error';
        return;
      }

      loginMessage.textContent = `Login realizado com sucesso para ${email}!`;
      loginMessage.className = 'form-message success';
    });
  }

  if (registerForm) {
    registerForm.addEventListener('submit', (event) => {
      event.preventDefault();

      const fullName = document.getElementById('fullName').value.trim();
      const email = document.getElementById('registerEmail').value.trim();
      const password = document.getElementById('registerPassword').value;
      const confirmPassword = document.getElementById('confirmPassword').value;

      if (!fullName || !email || !password || !confirmPassword) {
        registerMessage.textContent = 'Todos os campos são obrigatórios.';
        registerMessage.className = 'form-message error';
        return;
      }

      if (password.length < 6) {
        registerMessage.textContent = 'A senha deve ter pelo menos 6 caracteres.';
        registerMessage.className = 'form-message error';
        return;
      }

      if (password !== confirmPassword) {
        registerMessage.textContent = 'As senhas não coincidem.';
        registerMessage.className = 'form-message error';
        return;
      }

      registerMessage.textContent = `Conta criada com sucesso para ${fullName}!`;
      registerMessage.className = 'form-message success';
    });
  }

  toggleButtons.forEach((button) => {
    button.addEventListener('click', () => {
      const input = button.previousElementSibling;
      const shouldShow = input.type === 'password';
      input.type = shouldShow ? 'text' : 'password';
      button.textContent = shouldShow ? 'Ocultar' : 'Mostrar';
    });
  });
});
