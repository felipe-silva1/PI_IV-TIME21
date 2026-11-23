(() => {
    const TOKEN_KEY = 'guiavest.sessionToken';
    const USER_ID_KEY = 'guiavest.usuarioId';

    const siteAtivo = document.querySelector('#lembretes-site-ativo');
    const emailAtivo = document.querySelector('#lembretes-email-ativo');
    const statusSite = document.querySelector('#lembretes-site-status');
    const statusEmail = document.querySelector('#lembretes-email-status');
    const usuarioId = sessionStorage.getItem(USER_ID_KEY);
    const preferenciaSiteKey = `guiavest.lembretes.siteAtivo.${usuarioId}`;
    const preferenciaEmailKey = `guiavest.lembretes.emailAtivo.${usuarioId}`;

    if (!sessionStorage.getItem(TOKEN_KEY) || !usuarioId) {
        location.replace('../login.html');
        return;
    }

    function mostrarPermissaoSite() {
        if (!('Notification' in window)) {
            statusSite.textContent = 'Este navegador não oferece notificações.';
            siteAtivo.disabled = true;
        } else if (Notification.permission === 'denied') {
            statusSite.textContent =
                'As notificações estão bloqueadas nas permissões do navegador.';
        } else if (Notification.permission === 'default') {
            statusSite.textContent =
                'Ao ativar, o navegador solicitará permissão para mostrar os avisos.';
        } else {
            statusSite.textContent = siteAtivo.checked
                ? 'Notificações do site ativadas.'
                : 'Notificações do site desativadas.';
        }
    }

    siteAtivo.checked = localStorage.getItem(preferenciaSiteKey) === 'true';
    emailAtivo.checked = localStorage.getItem(preferenciaEmailKey) === 'true';
    mostrarPermissaoSite();
    statusEmail.textContent = emailAtivo.checked
        ? 'Preferência de e-mail ativada neste navegador.'
        : 'Preferência de e-mail desativada neste navegador.';

    siteAtivo.addEventListener('change', async () => {
        if (siteAtivo.checked && 'Notification' in window &&
            Notification.permission === 'default') {
            try {
                await Notification.requestPermission();
            } catch (erro) {
                console.warn('Não foi possível solicitar permissão para notificações.', erro);
            }
        }

        if (siteAtivo.checked &&
            (!('Notification' in window) || Notification.permission !== 'granted')) {
            siteAtivo.checked = false;
            localStorage.setItem(preferenciaSiteKey, 'false');
        } else {
            localStorage.setItem(preferenciaSiteKey, String(siteAtivo.checked));
        }
        mostrarPermissaoSite();
    });

    emailAtivo.addEventListener('change', () => {
        localStorage.setItem(preferenciaEmailKey, String(emailAtivo.checked));
        statusEmail.textContent = emailAtivo.checked
            ? 'Preferência de e-mail ativada neste navegador.'
            : 'Preferência de e-mail desativada neste navegador.';
    });
})();
