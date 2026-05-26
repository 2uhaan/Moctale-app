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

                  if (
                      data &&
                      data.files &&
                      data.files.length > 0
                  ) {

                      const file = data.files[0];

                      const base64 =
                          await fileToBase64(file);

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

                  console.error(
                      'Moctale share failed',
                      e
                  );
              }
          }

          try {

              Object.defineProperty(
                  navigator,
                  'share',
                  {
                      value: shareFunc,
                      writable: true,
                      configurable: true
                  }
              );

              Object.defineProperty(
                  navigator,
                  'canShare',
                  {
                      value: function(data) {

                          if (
                              data &&
                              data.files
                          ) {
                              return true;
                          }

                          return true;
                      },
                      writable: true,
                      configurable: true
                  }
              );

          } catch (e) {

              navigator.share = shareFunc;

              navigator.canShare = function() {
                  return true;
              };
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

              const isDownload =
                  anchor.hasAttribute('download');

              const isBlob =
                  href.startsWith('blob:');

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

                  if (!blob.type.includes('png')) {
                      return;
                  }

                  const base64 =
                      await blobToBase64(blob);

                  if (window.AndroidDownloader) {

                      window.AndroidDownloader
                          .downloadBase64Image(base64);
                  }

              } catch (e) {

                  console.error(
                      'Moctale download failed',
                      e
                  );
              }

          }, true);

      })();
      """
            .trimIndent()

    val injectVideoFixScript =
        """
      (function() {

          function patchVideos() {

              const videos =
                  document.querySelectorAll('video');

              videos.forEach(video => {

                  video.setAttribute(
                      'playsinline',
                      ''
                  );

                  video.setAttribute(
                      'webkit-playsinline',
                      ''
                  );

                  video.playsInline = true;

                  if (
                      video.autoplay ||
                      video.loop ||
                      video.muted
                  ) {

                      video.controls = false;

                      video.removeAttribute(
                          'controls'
                      );
                  }

                  video.preload = 'auto';

                  if (
                      !video.hasAttribute('poster')
                  ) {

                      video.setAttribute(
                          'poster',
                          'data:image/gif;base64,R0lGODlhAQABAAD/ACwAAAAAAQABAAACADs='
                      );
                  }
              });
          }

          patchVideos();

          const observer =
              new MutationObserver(() => {
                  patchVideos();
              });

          observer.observe(
              document.documentElement,
              {
                  childList: true,
                  subtree: true
              }
          );

      })();
      """
            .trimIndent()
}
