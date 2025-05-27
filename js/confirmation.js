// Create confirmation modal
const createConfirmModal = () => {
    const modal = document.createElement('div');
    modal.id = 'confirm-action-modal';
    modal.className = 'modal';
    modal.style.display = 'none';
    modal.innerHTML = `
            <div class="modal-content">
                <span class="close">×</span>
                <h3 id="confirm-action-title">Confirm Action</h3>
                <p id="confirm-action-message">Are you sure?</p>
                <div class="button-group">
                    <button id="confirm-action-btn">Confirm</button>
                    <button class="cancel">Cancel</button>
                </div>
            </div>
        `;
    document.body.appendChild(modal);
    return modal;
};

// Show confirmation modal
const showConfirmModal = (title, message, onConfirm) => {
    const confirmModal = document.getElementById('confirm-action-modal') || createConfirmModal();
    const titleEl = confirmModal.querySelector('#confirm-action-title');
    const messageEl = confirmModal.querySelector('#confirm-action-message');
    const confirmBtn = confirmModal.querySelector('#confirm-action-btn');
    const cancelBtn = confirmModal.querySelector('.cancel');
    const closeBtn = confirmModal.querySelector('.close');

    titleEl.textContent = title;
    messageEl.textContent = message;
    confirmModal.style.display = 'flex';

    const closeModal = () => {
        confirmModal.style.display = 'none';
    };

    confirmBtn.onclick = async () => {
        await onConfirm();
        closeModal();
    };

    cancelBtn.onclick = closeModal;
    closeBtn.onclick = closeModal;
};

export {showConfirmModal}