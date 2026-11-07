
const Gestures = {
    enableSwipeToClose(backdrop, onClose) {
        const sheet = backdrop.querySelector('.modal-window');
        if (!sheet) return;

        if (!sheet.querySelector('.sheet-handle')) {
            const handle = document.createElement('div');
            handle.className = 'sheet-handle';
            handle.setAttribute('aria-hidden', 'true');
            sheet.prepend(handle);
        }

        let startY = 0;
        let startX = 0;
        let startTime = 0;
        let dragging = false;
        let decided = false;
        let dy = 0;

        sheet.addEventListener('touchstart', (e) => {
            if (e.touches.length !== 1) return;
            if (sheet.scrollTop > 0) return;
            startY = e.touches[0].clientY;
            startX = e.touches[0].clientX;
            startTime = Date.now();
            dragging = true;
            decided = false;
            dy = 0;
        }, { passive: true });

        sheet.addEventListener('touchmove', (e) => {
            if (!dragging) return;
            const moveY = e.touches[0].clientY - startY;
            const moveX = e.touches[0].clientX - startX;

            if (!decided) {
                if (Math.abs(moveY) < 8 && Math.abs(moveX) < 8) return;
                decided = true;
                if (moveY <= 0 || Math.abs(moveX) > Math.abs(moveY)) {
                    dragging = false;
                    return;
                }
                sheet.classList.add('is-dragging');
            }

            dy = Math.max(0, moveY);
            sheet.style.transform = `translateY(${dy}px)`;
            backdrop.style.backgroundColor = `rgba(15, 31, 61, ${Math.max(0.1, 0.5 - dy / 800)})`;
            if (e.cancelable) e.preventDefault();
        }, { passive: false });

        const finish = () => {
            if (!dragging) return;
            dragging = false;
            sheet.classList.remove('is-dragging');
            const velocity = dy / Math.max(1, Date.now() - startTime);
            const shouldClose = dy > Math.min(140, sheet.offsetHeight * 0.25) || (dy > 40 && velocity > 0.6);

            if (shouldClose) {
                sheet.style.transition = 'transform 0.2s ease-in';
                sheet.style.transform = 'translateY(100%)';
                setTimeout(() => {
                    resetSheet();
                    onClose();
                }, 190);
            } else {
                sheet.style.transition = 'transform 0.25s cubic-bezier(.2, .8, .2, 1)';
                sheet.style.transform = 'translateY(0)';
                backdrop.style.backgroundColor = '';
                setTimeout(resetSheet, 260);
            }
        };

        function resetSheet() {
            sheet.style.transition = '';
            sheet.style.transform = '';
            backdrop.style.backgroundColor = '';
        }

        sheet.addEventListener('touchend', finish);
        sheet.addEventListener('touchcancel', finish);
    },

    enableTabSwipe(area, getCurrent, goTo, count) {
        let startX = 0;
        let startY = 0;
        let startTime = 0;
        let tracking = false;

        area.addEventListener('touchstart', (e) => {
            tracking = false;
            if (e.touches.length !== 1) return;
            if (document.querySelector('.modal-backdrop:not(.hidden)')) return;
            if (e.target.closest('[data-no-tab-swipe]')) return;
            startX = e.touches[0].clientX;
            startY = e.touches[0].clientY;
            startTime = Date.now();
            tracking = true;
        }, { passive: true });

        area.addEventListener('touchend', (e) => {
            if (!tracking) return;
            tracking = false;
            const t = e.changedTouches[0];
            const dx = t.clientX - startX;
            const dy = t.clientY - startY;
            const fastEnough = Date.now() - startTime < 600;
            if (!fastEnough || Math.abs(dx) < 70 || Math.abs(dx) < Math.abs(dy) * 1.5) return;

            const current = getCurrent();
            const next = dx < 0 ? current + 1 : current - 1;
            if (next < 0 || next >= count) return;
            goTo(next, dx < 0 ? 'left' : 'right');
        }, { passive: true });
    }
};
