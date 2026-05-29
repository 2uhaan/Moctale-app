package com.ruhaan.moctale.core.webview

object WebViewScripts {
  val injectShareScript =
      """
      (function() {
          async function fileToBase64(file) {
              return new Promise((resolve, reject) => {
                  const reader = new FileReader();
                  reader.onloadend = () => resolve(reader.result);
                  reader.onerror = reject;
                  reader.readAsDataURL(file);
              });
          }
          async function shareFunc(data) {
              try {
                  if (data && data.files && data.files.length > 0) {
                      const file = data.files[0];
                      const base64 = await fileToBase64(file);
                      if (window.AndroidShare) {
                          window.AndroidShare.shareImage(
                              base64,
                              data.title || '',
                              data.text || ''
                          );
                          return;
                      }
                  }
                  if (window.AndroidShare) {
                      window.AndroidShare.share(
                          data?.title || '',
                          data?.text || '',
                          data?.url || window.location.href
                      );
                  }
              } catch (e) {
                  console.error('Moctale share failed', e);
              }
          }
          try {
              Object.defineProperty(navigator, 'share', {
                  value: shareFunc,
                  writable: true,
                  configurable: true
              });
              Object.defineProperty(navigator, 'canShare', {
                  value: function(data) {
                      return true;
                  },
                  writable: true,
                  configurable: true
              });
          } catch (e) {
              navigator.share = shareFunc;
              navigator.canShare = function() { return true; };
          }
      })();
      """
          .trimIndent()

  val injectDownloadScript =
      """
      (function() {
          if (window.__MOCTALE_DOWNLOAD_PATCHED__) return;
          window.__MOCTALE_DOWNLOAD_PATCHED__ = true;
          
          async function blobToBase64(blob) {
              return new Promise((resolve, reject) => {
                  const reader = new FileReader();
                  reader.onloadend = () => resolve(reader.result);
                  reader.onerror = reject;
                  reader.readAsDataURL(blob);
              });
          }
          
          document.addEventListener('click', async function(event) {
              const anchor = event.target.closest('a');
              if (!anchor) return;
              
              const href = anchor.href || '';
              const isDownload = anchor.hasAttribute('download');
              const isBlob = href.startsWith('blob:');
              if (!isDownload && !isBlob) return;
              
              try {
                  event.preventDefault();
                  let blob;
                  if (isBlob) {
                      const response = await fetch(href);
                      blob = await response.blob();
                  } else {
                      const response = await fetch(href);
                      blob = await response.blob();
                  }
                  
                  if (!blob.type.includes('png')) return;
                  
                  const base64 = await blobToBase64(blob);
                  if (window.AndroidDownloader) {
                      window.AndroidDownloader.downloadBase64Image(base64);
                  }
              } catch (e) {
                  console.error('Moctale download failed', e);
              }
          }, true);
      })();
      """
          .trimIndent()

  val injectVideoFixScript =
      """
      (function() {
          if (window.__MOCTALE_VIDEO_PATCHED__) return;
          window.__MOCTALE_VIDEO_PATCHED__ = true;
          
          function patchVideo(video) {
              if (video.__moctale_patched__) return;
              video.__moctale_patched__ = true;
              
              video.playsInline = true;
              video.setAttribute('playsinline', '');
              video.setAttribute('webkit-playsinline', '');
              
              if (video.autoplay || video.loop || video.muted) {
                  video.controls = false;
                  video.removeAttribute('controls');
              }
              
              video.preload = 'metadata';
              if (!video.poster) {
                  video.poster = 'data:image/gif;base64,R0lGODlhAQABAAD/ACwAAAAAAQABAAACADs=';
              }
          }
          
          document.querySelectorAll('video').forEach(patchVideo);
          
          const observer = new MutationObserver((mutations) => {
              for (const mutation of mutations) {
                  for (const node of mutation.addedNodes) {
                      if (node.nodeType !== 1) continue;
                      if (node.tagName === 'VIDEO') {
                          patchVideo(node);
                      }
                      node.querySelectorAll?.('video').forEach(patchVideo);
                  }
              }
          });
          
          observer.observe(document.body, {
              childList: true,
              subtree: true
          });
      })();
      """
          .trimIndent()

  val injectScrollBridgeScript =
      """
      (function() {
          if (window.__MOCTALE_SCROLL_BRIDGE_PATCHED__) return;
          window.__MOCTALE_SCROLL_BRIDGE_PATCHED__ = true;
          
          let ticking = false;
          let lastState = null;
          
          function sendState(atTop) {
              if (lastState === atTop) return;
              lastState = atTop;
              window.ScrollBridge?.reportScrollTop(atTop);
          }
          
          function getScrollTop(target) {
              if (
                  target === document ||
                  target === window ||
                  target === document.documentElement ||
                  target === document.body
              ) {
                  return (document.scrollingElement || document.documentElement).scrollTop;
              }
              return target.scrollTop || 0;
          }
          
          function handleScroll(event) {
              if (ticking) return;
              ticking = true;
              
              requestAnimationFrame(() => {
                  const target = event.target;
                  const scrollTop = getScrollTop(target);
                  sendState(scrollTop <= 10);
                  ticking = false;
              });
          }
          
          // Capture ALL scroll events everywhere
          window.addEventListener('scroll', handleScroll, {
              passive: true,
              capture: true
          });
          
          // Initial state check
          sendState((document.scrollingElement || document.documentElement).scrollTop <= 10);
      })();
      """
          .trimIndent()
}
