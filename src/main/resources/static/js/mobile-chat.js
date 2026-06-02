/** Mobile split-view chat: list ↔ panel (one visible at a time below 768px) */

export const MOBILE_CHAT_MAX_WIDTH = 767;

export const isMobileSplitView = () =>
  window.matchMedia(`(max-width: ${MOBILE_CHAT_MAX_WIDTH}px)`).matches;

export const openMobileChatPanel = (viewKey) => {
  if (!isMobileSplitView()) return;
  const section = document.querySelector(`.view-${viewKey}.split-chat-layout`);
  section?.classList.add('mobile-panel-open');
};

export const closeMobileChatPanel = (viewKey) => {
  document.querySelector(`.view-${viewKey}.split-chat-layout`)?.classList.remove('mobile-panel-open');
};

export const resetAllMobileChatPanels = () => {
  document.querySelectorAll('.split-chat-layout.mobile-panel-open').forEach((el) => {
    el.classList.remove('mobile-panel-open');
  });
};

export const initMobileChatLayout = () => {
  document.querySelectorAll('[data-mobile-chat-back]').forEach((btn) => {
    btn.addEventListener('click', () => {
      closeMobileChatPanel(btn.dataset.mobileChatBack);
    });
  });

  window.matchMedia(`(max-width: ${MOBILE_CHAT_MAX_WIDTH}px)`).addEventListener('change', (e) => {
    if (!e.matches) resetAllMobileChatPanels();
  });
};
