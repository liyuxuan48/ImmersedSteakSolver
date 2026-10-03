'use strict';
const CACHE='heat-lab-github-v6-doneness';
const ASSETS=['./','./index.html','./style.css','./app.js','./doneness.js','./i18n.js','./manifest.en.webmanifest','./export.js','./solver.js','./worker.js','./manifest.webmanifest','./icon-192.png','./icon-512.png'];
self.addEventListener('install',e=>e.waitUntil(caches.open(CACHE).then(c=>c.addAll(ASSETS))));
self.addEventListener('activate',e=>e.waitUntil(caches.keys().then(keys=>Promise.all(keys.filter(k=>k.startsWith('heat-lab-github-')&&k!==CACHE).map(k=>caches.delete(k)))).then(()=>self.clients.claim())));
self.addEventListener('fetch',e=>{const u=new URL(e.request.url);if(e.request.method!=='GET'||u.origin!==location.origin||!ASSETS.some(p=>new URL(p,location.href).pathname===u.pathname))return;e.respondWith(caches.match(e.request).then(hit=>hit||fetch(e.request)));});
