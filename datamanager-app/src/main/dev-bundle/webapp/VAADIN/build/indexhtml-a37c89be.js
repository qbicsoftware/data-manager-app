(function(){const e=document.createElement("link").relList;if(e&&e.supports&&e.supports("modulepreload"))return;for(const a of document.querySelectorAll('link[rel="modulepreload"]'))i(a);new MutationObserver(a=>{for(const n of a)if(n.type==="childList")for(const r of n.addedNodes)r.tagName==="LINK"&&r.rel==="modulepreload"&&i(r)}).observe(document,{childList:!0,subtree:!0});function o(a){const n={};return a.integrity&&(n.integrity=a.integrity),a.referrerPolicy&&(n.referrerPolicy=a.referrerPolicy),a.crossOrigin==="use-credentials"?n.credentials="include":a.crossOrigin==="anonymous"?n.credentials="omit":n.credentials="same-origin",n}function i(a){if(a.ep)return;a.ep=!0;const n=o(a);fetch(a.href,n)}})();window.Vaadin=window.Vaadin||{};window.Vaadin.featureFlags=window.Vaadin.featureFlags||{};window.Vaadin.featureFlags.exampleFeatureFlag=!1;window.Vaadin.featureFlags.collaborationEngineBackend=!1;window.Vaadin.featureFlags.webPush=!1;window.Vaadin.featureFlags.formFillerAddon=!1;const Ma="modulepreload",Va=function(t,e){return new URL(t,e).href},ni={},v=function(e,o,i){if(!o||o.length===0)return e();const a=document.getElementsByTagName("link");return Promise.all(o.map(n=>{if(n=Va(n,i),n in ni)return;ni[n]=!0;const r=n.endsWith(".css"),l=r?'[rel="stylesheet"]':"";if(!!i)for(let d=a.length-1;d>=0;d--){const h=a[d];if(h.href===n&&(!r||h.rel==="stylesheet"))return}else if(document.querySelector(`link[href="${n}"]${l}`))return;const c=document.createElement("link");if(c.rel=r?"stylesheet":Ma,r||(c.as="script",c.crossOrigin=""),c.href=n,document.head.appendChild(c),r)return new Promise((d,h)=>{c.addEventListener("load",d),c.addEventListener("error",()=>h(new Error(`Unable to preload CSS for ${n}`)))})})).then(()=>e()).catch(n=>{const r=new Event("vite:preloadError",{cancelable:!0});if(r.payload=n,window.dispatchEvent(r),!r.defaultPrevented)throw n})};function Et(t){return t=t||[],Array.isArray(t)?t:[t]}function ee(t){return`[Vaadin.Router] ${t}`}function Da(t){if(typeof t!="object")return String(t);const e=Object.prototype.toString.call(t).match(/ (.*)\]$/)[1];return e==="Object"||e==="Array"?`${e} ${JSON.stringify(t)}`:e}const St="module",Ct="nomodule",_o=[St,Ct];function ri(t){if(!t.match(/.+\.[m]?js$/))throw new Error(ee(`Unsupported type for bundle "${t}": .js or .mjs expected.`))}function oa(t){if(!t||!Q(t.path))throw new Error(ee('Expected route config to be an object with a "path" string property, or an array of such objects'));const e=t.bundle,o=["component","redirect","bundle"];if(!Se(t.action)&&!Array.isArray(t.children)&&!Se(t.children)&&!zt(e)&&!o.some(i=>Q(t[i])))throw new Error(ee(`Expected route config "${t.path}" to include either "${o.join('", "')}" or "action" function but none found.`));if(e)if(Q(e))ri(e);else if(_o.some(i=>i in e))_o.forEach(i=>i in e&&ri(e[i]));else throw new Error(ee('Expected route bundle to include either "'+Ct+'" or "'+St+'" keys, or both'));t.redirect&&["bundle","component"].forEach(i=>{i in t&&console.warn(ee(`Route config "${t.path}" has both "redirect" and "${i}" properties, and "redirect" will always override the latter. Did you mean to only use "${i}"?`))})}function si(t){Et(t).forEach(e=>oa(e))}function li(t,e){let o=document.head.querySelector('script[src="'+t+'"][async]');return o||(o=document.createElement("script"),o.setAttribute("src",t),e===St?o.setAttribute("type",St):e===Ct&&o.setAttribute(Ct,""),o.async=!0),new Promise((i,a)=>{o.onreadystatechange=o.onload=n=>{o.__dynamicImportLoaded=!0,i(n)},o.onerror=n=>{o.parentNode&&o.parentNode.removeChild(o),a(n)},o.parentNode===null?document.head.appendChild(o):o.__dynamicImportLoaded&&i()})}function Ua(t){return Q(t)?li(t):Promise.race(_o.filter(e=>e in t).map(e=>li(t[e],e)))}function Ye(t,e){return!window.dispatchEvent(new CustomEvent(`vaadin-router-${t}`,{cancelable:t==="go",detail:e}))}function zt(t){return typeof t=="object"&&!!t}function Se(t){return typeof t=="function"}function Q(t){return typeof t=="string"}function ia(t){const e=new Error(ee(`Page not found (${t.pathname})`));return e.context=t,e.code=404,e}const Oe=new class{};function qa(t){const e=t.port,o=t.protocol,n=o==="http:"&&e==="80"||o==="https:"&&e==="443"?t.hostname:t.host;return`${o}//${n}`}function ci(t){if(t.defaultPrevented||t.button!==0||t.shiftKey||t.ctrlKey||t.altKey||t.metaKey)return;let e=t.target;const o=t.composedPath?t.composedPath():t.path||[];for(let l=0;l<o.length;l++){const s=o[l];if(s.nodeName&&s.nodeName.toLowerCase()==="a"){e=s;break}}for(;e&&e.nodeName.toLowerCase()!=="a";)e=e.parentNode;if(!e||e.nodeName.toLowerCase()!=="a"||e.target&&e.target.toLowerCase()!=="_self"||e.hasAttribute("download")||e.hasAttribute("router-ignore")||e.pathname===window.location.pathname&&e.hash!==""||(e.origin||qa(e))!==window.location.origin)return;const{pathname:a,search:n,hash:r}=e;Ye("go",{pathname:a,search:n,hash:r})&&(t.preventDefault(),t&&t.type==="click"&&window.scrollTo(0,0))}const Fa={activate(){window.document.addEventListener("click",ci)},inactivate(){window.document.removeEventListener("click",ci)}},Ba=/Trident/.test(navigator.userAgent);Ba&&!Se(window.PopStateEvent)&&(window.PopStateEvent=function(t,e){e=e||{};var o=document.createEvent("Event");return o.initEvent(t,!!e.bubbles,!!e.cancelable),o.state=e.state||null,o},window.PopStateEvent.prototype=window.Event.prototype);function di(t){if(t.state==="vaadin-router-ignore")return;const{pathname:e,search:o,hash:i}=window.location;Ye("go",{pathname:e,search:o,hash:i})}const Ha={activate(){window.addEventListener("popstate",di)},inactivate(){window.removeEventListener("popstate",di)}};var He=ca,Wa=Po,Ga=Xa,Ka=ra,Ya=la,aa="/",na="./",Ja=new RegExp(["(\\\\.)","(?:\\:(\\w+)(?:\\(((?:\\\\.|[^\\\\()])+)\\))?|\\(((?:\\\\.|[^\\\\()])+)\\))([+*?])?"].join("|"),"g");function Po(t,e){for(var o=[],i=0,a=0,n="",r=e&&e.delimiter||aa,l=e&&e.delimiters||na,s=!1,c;(c=Ja.exec(t))!==null;){var d=c[0],h=c[1],m=c.index;if(n+=t.slice(a,m),a=m+d.length,h){n+=h[1],s=!0;continue}var x="",pe=t[a],ue=c[2],re=c[3],qt=c[4],W=c[5];if(!s&&n.length){var oe=n.length-1;l.indexOf(n[oe])>-1&&(x=n[oe],n=n.slice(0,oe))}n&&(o.push(n),n="",s=!1);var $e=x!==""&&pe!==void 0&&pe!==x,je=W==="+"||W==="*",Ft=W==="?"||W==="*",se=x||r,lt=re||qt;o.push({name:ue||i++,prefix:x,delimiter:se,optional:Ft,repeat:je,partial:$e,pattern:lt?Qa(lt):"[^"+he(se)+"]+?"})}return(n||a<t.length)&&o.push(n+t.substr(a)),o}function Xa(t,e){return ra(Po(t,e))}function ra(t){for(var e=new Array(t.length),o=0;o<t.length;o++)typeof t[o]=="object"&&(e[o]=new RegExp("^(?:"+t[o].pattern+")$"));return function(i,a){for(var n="",r=a&&a.encode||encodeURIComponent,l=0;l<t.length;l++){var s=t[l];if(typeof s=="string"){n+=s;continue}var c=i?i[s.name]:void 0,d;if(Array.isArray(c)){if(!s.repeat)throw new TypeError('Expected "'+s.name+'" to not repeat, but got array');if(c.length===0){if(s.optional)continue;throw new TypeError('Expected "'+s.name+'" to not be empty')}for(var h=0;h<c.length;h++){if(d=r(c[h],s),!e[l].test(d))throw new TypeError('Expected all "'+s.name+'" to match "'+s.pattern+'"');n+=(h===0?s.prefix:s.delimiter)+d}continue}if(typeof c=="string"||typeof c=="number"||typeof c=="boolean"){if(d=r(String(c),s),!e[l].test(d))throw new TypeError('Expected "'+s.name+'" to match "'+s.pattern+'", but got "'+d+'"');n+=s.prefix+d;continue}if(s.optional){s.partial&&(n+=s.prefix);continue}throw new TypeError('Expected "'+s.name+'" to be '+(s.repeat?"an array":"a string"))}return n}}function he(t){return t.replace(/([.+*?=^!:${}()[\]|/\\])/g,"\\$1")}function Qa(t){return t.replace(/([=!:$/()])/g,"\\$1")}function sa(t){return t&&t.sensitive?"":"i"}function Za(t,e){if(!e)return t;var o=t.source.match(/\((?!\?)/g);if(o)for(var i=0;i<o.length;i++)e.push({name:i,prefix:null,delimiter:null,optional:!1,repeat:!1,partial:!1,pattern:null});return t}function en(t,e,o){for(var i=[],a=0;a<t.length;a++)i.push(ca(t[a],e,o).source);return new RegExp("(?:"+i.join("|")+")",sa(o))}function tn(t,e,o){return la(Po(t,o),e,o)}function la(t,e,o){o=o||{};for(var i=o.strict,a=o.start!==!1,n=o.end!==!1,r=he(o.delimiter||aa),l=o.delimiters||na,s=[].concat(o.endsWith||[]).map(he).concat("$").join("|"),c=a?"^":"",d=t.length===0,h=0;h<t.length;h++){var m=t[h];if(typeof m=="string")c+=he(m),d=h===t.length-1&&l.indexOf(m[m.length-1])>-1;else{var x=m.repeat?"(?:"+m.pattern+")(?:"+he(m.delimiter)+"(?:"+m.pattern+"))*":m.pattern;e&&e.push(m),m.optional?m.partial?c+=he(m.prefix)+"("+x+")?":c+="(?:"+he(m.prefix)+"("+x+"))?":c+=he(m.prefix)+"("+x+")"}}return n?(i||(c+="(?:"+r+")?"),c+=s==="$"?"$":"(?="+s+")"):(i||(c+="(?:"+r+"(?="+s+"))?"),d||(c+="(?="+r+"|"+s+")")),new RegExp(c,sa(o))}function ca(t,e,o){return t instanceof RegExp?Za(t,e):Array.isArray(t)?en(t,e,o):tn(t,e,o)}He.parse=Wa;He.compile=Ga;He.tokensToFunction=Ka;He.tokensToRegExp=Ya;const{hasOwnProperty:on}=Object.prototype,ko=new Map;ko.set("|false",{keys:[],pattern:/(?:)/});function mi(t){try{return decodeURIComponent(t)}catch{return t}}function an(t,e,o,i,a){o=!!o;const n=`${t}|${o}`;let r=ko.get(n);if(!r){const c=[];r={keys:c,pattern:He(t,c,{end:o,strict:t===""})},ko.set(n,r)}const l=r.pattern.exec(e);if(!l)return null;const s=Object.assign({},a);for(let c=1;c<l.length;c++){const d=r.keys[c-1],h=d.name,m=l[c];(m!==void 0||!on.call(s,h))&&(d.repeat?s[h]=m?m.split(d.delimiter).map(mi):[]:s[h]=m&&mi(m))}return{path:l[0],keys:(i||[]).concat(r.keys),params:s}}function da(t,e,o,i,a){let n,r,l=0,s=t.path||"";return s.charAt(0)==="/"&&(o&&(s=s.substr(1)),o=!0),{next(c){if(t===c)return{done:!0};const d=t.__children=t.__children||t.children;if(!n&&(n=an(s,e,!d,i,a),n))return{done:!1,value:{route:t,keys:n.keys,params:n.params,path:n.path}};if(n&&d)for(;l<d.length;){if(!r){const m=d[l];m.parent=t;let x=n.path.length;x>0&&e.charAt(x)==="/"&&(x+=1),r=da(m,e.substr(x),o,n.keys,n.params)}const h=r.next(c);if(!h.done)return{done:!1,value:h.value};r=null,l++}return{done:!0}}}}function nn(t){if(Se(t.route.action))return t.route.action(t)}function rn(t,e){let o=e;for(;o;)if(o=o.parent,o===t)return!0;return!1}function sn(t){let e=`Path '${t.pathname}' is not properly resolved due to an error.`;const o=(t.route||{}).path;return o&&(e+=` Resolution had failed on route: '${o}'`),e}function ln(t,e){const{route:o,path:i}=e;if(o&&!o.__synthetic){const a={path:i,route:o};if(!t.chain)t.chain=[];else if(o.parent){let n=t.chain.length;for(;n--&&t.chain[n].route&&t.chain[n].route!==o.parent;)t.chain.pop()}t.chain.push(a)}}class Xe{constructor(e,o={}){if(Object(e)!==e)throw new TypeError("Invalid routes");this.baseUrl=o.baseUrl||"",this.errorHandler=o.errorHandler,this.resolveRoute=o.resolveRoute||nn,this.context=Object.assign({resolver:this},o.context),this.root=Array.isArray(e)?{path:"",__children:e,parent:null,__synthetic:!0}:e,this.root.parent=null}getRoutes(){return[...this.root.__children]}setRoutes(e){si(e);const o=[...Et(e)];this.root.__children=o}addRoutes(e){return si(e),this.root.__children.push(...Et(e)),this.getRoutes()}removeRoutes(){this.setRoutes([])}resolve(e){const o=Object.assign({},this.context,Q(e)?{pathname:e}:e),i=da(this.root,this.__normalizePathname(o.pathname),this.baseUrl),a=this.resolveRoute;let n=null,r=null,l=o;function s(c,d=n.value.route,h){const m=h===null&&n.value.route;return n=r||i.next(m),r=null,!c&&(n.done||!rn(d,n.value.route))?(r=n,Promise.resolve(Oe)):n.done?Promise.reject(ia(o)):(l=Object.assign(l?{chain:l.chain?l.chain.slice(0):[]}:{},o,n.value),ln(l,n.value),Promise.resolve(a(l)).then(x=>x!=null&&x!==Oe?(l.result=x.result||x,l):s(c,d,x)))}return o.next=s,Promise.resolve().then(()=>s(!0,this.root)).catch(c=>{const d=sn(l);if(c?console.warn(d):c=new Error(d),c.context=c.context||l,c instanceof DOMException||(c.code=c.code||500),this.errorHandler)return l.result=this.errorHandler(c),l;throw c})}static __createUrl(e,o){return new URL(e,o)}get __effectiveBaseUrl(){return this.baseUrl?this.constructor.__createUrl(this.baseUrl,document.baseURI||document.URL).href.replace(/[^\/]*$/,""):""}__normalizePathname(e){if(!this.baseUrl)return e;const o=this.__effectiveBaseUrl,i=this.constructor.__createUrl(e,o).href;if(i.slice(0,o.length)===o)return i.slice(o.length)}}Xe.pathToRegexp=He;const{pathToRegexp:pi}=Xe,ui=new Map;function ma(t,e,o){const i=e.name||e.component;if(i&&(t.has(i)?t.get(i).push(e):t.set(i,[e])),Array.isArray(o))for(let a=0;a<o.length;a++){const n=o[a];n.parent=e,ma(t,n,n.__children||n.children)}}function hi(t,e){const o=t.get(e);if(o&&o.length>1)throw new Error(`Duplicate route with name "${e}". Try seting unique 'name' route properties.`);return o&&o[0]}function gi(t){let e=t.path;return e=Array.isArray(e)?e[0]:e,e!==void 0?e:""}function cn(t,e={}){if(!(t instanceof Xe))throw new TypeError("An instance of Resolver is expected");const o=new Map;return(i,a)=>{let n=hi(o,i);if(!n&&(o.clear(),ma(o,t.root,t.root.__children),n=hi(o,i),!n))throw new Error(`Route "${i}" not found`);let r=ui.get(n.fullPath);if(!r){let s=gi(n),c=n.parent;for(;c;){const x=gi(c);x&&(s=x.replace(/\/$/,"")+"/"+s.replace(/^\//,"")),c=c.parent}const d=pi.parse(s),h=pi.tokensToFunction(d),m=Object.create(null);for(let x=0;x<d.length;x++)Q(d[x])||(m[d[x].name]=!0);r={toPath:h,keys:m},ui.set(s,r),n.fullPath=s}let l=r.toPath(a,e)||"/";if(e.stringifyQueryParams&&a){const s={},c=Object.keys(a);for(let h=0;h<c.length;h++){const m=c[h];r.keys[m]||(s[m]=a[m])}const d=e.stringifyQueryParams(s);d&&(l+=d.charAt(0)==="?"?d:`?${d}`)}return l}}let fi=[];function dn(t){fi.forEach(e=>e.inactivate()),t.forEach(e=>e.activate()),fi=t}const mn=t=>{const e=getComputedStyle(t).getPropertyValue("animation-name");return e&&e!=="none"},pn=(t,e)=>{const o=()=>{t.removeEventListener("animationend",o),e()};t.addEventListener("animationend",o)};function vi(t,e){return t.classList.add(e),new Promise(o=>{if(mn(t)){const i=t.getBoundingClientRect(),a=`height: ${i.bottom-i.top}px; width: ${i.right-i.left}px`;t.setAttribute("style",`position: absolute; ${a}`),pn(t,()=>{t.classList.remove(e),t.removeAttribute("style"),o()})}else t.classList.remove(e),o()})}const un=256;function Gt(t){return t!=null}function hn(t){const e=Object.assign({},t);return delete e.next,e}function Y({pathname:t="",search:e="",hash:o="",chain:i=[],params:a={},redirectFrom:n,resolver:r},l){const s=i.map(c=>c.route);return{baseUrl:r&&r.baseUrl||"",pathname:t,search:e,hash:o,routes:s,route:l||s.length&&s[s.length-1]||null,params:a,redirectFrom:n,getUrl:(c={})=>yt(ge.pathToRegexp.compile(pa(s))(Object.assign({},a,c)),r)}}function xi(t,e){const o=Object.assign({},t.params);return{redirect:{pathname:e,from:t.pathname,params:o}}}function gn(t,e){e.location=Y(t);const o=t.chain.map(i=>i.route).indexOf(t.route);return t.chain[o].element=e,e}function xt(t,e,o){if(Se(t))return t.apply(o,e)}function yi(t,e,o){return i=>{if(i&&(i.cancel||i.redirect))return i;if(o)return xt(o[t],e,o)}}function fn(t,e){if(!Array.isArray(t)&&!zt(t))throw new Error(ee(`Incorrect "children" value for the route ${e.path}: expected array or object, but got ${t}`));e.__children=[];const o=Et(t);for(let i=0;i<o.length;i++)oa(o[i]),e.__children.push(o[i])}function ut(t){if(t&&t.length){const e=t[0].parentNode;for(let o=0;o<t.length;o++)e.removeChild(t[o])}}function yt(t,e){const o=e.__effectiveBaseUrl;return o?e.constructor.__createUrl(t.replace(/^\//,""),o).pathname:t}function pa(t){return t.map(e=>e.path).reduce((e,o)=>o.length?e.replace(/\/$/,"")+"/"+o.replace(/^\//,""):e,"")}class ge extends Xe{constructor(e,o){const i=document.head.querySelector("base"),a=i&&i.getAttribute("href");super([],Object.assign({baseUrl:a&&Xe.__createUrl(a,document.URL).pathname.replace(/[^\/]*$/,"")},o)),this.resolveRoute=r=>this.__resolveRoute(r);const n=ge.NavigationTrigger;ge.setTriggers.apply(ge,Object.keys(n).map(r=>n[r])),this.baseUrl,this.ready,this.ready=Promise.resolve(e),this.location,this.location=Y({resolver:this}),this.__lastStartedRenderId=0,this.__navigationEventHandler=this.__onNavigationEvent.bind(this),this.setOutlet(e),this.subscribe(),this.__createdByRouter=new WeakMap,this.__addedByRouter=new WeakMap}__resolveRoute(e){const o=e.route;let i=Promise.resolve();Se(o.children)&&(i=i.then(()=>o.children(hn(e))).then(n=>{!Gt(n)&&!Se(o.children)&&(n=o.children),fn(n,o)}));const a={redirect:n=>xi(e,n),component:n=>{const r=document.createElement(n);return this.__createdByRouter.set(r,!0),r}};return i.then(()=>{if(this.__isLatestRender(e))return xt(o.action,[e,a],o)}).then(n=>{if(Gt(n)&&(n instanceof HTMLElement||n.redirect||n===Oe))return n;if(Q(o.redirect))return a.redirect(o.redirect);if(o.bundle)return Ua(o.bundle).then(()=>{},()=>{throw new Error(ee(`Bundle not found: ${o.bundle}. Check if the file name is correct`))})}).then(n=>{if(Gt(n))return n;if(Q(o.component))return a.component(o.component)})}setOutlet(e){e&&this.__ensureOutlet(e),this.__outlet=e}getOutlet(){return this.__outlet}setRoutes(e,o=!1){return this.__previousContext=void 0,this.__urlForName=void 0,super.setRoutes(e),o||this.__onNavigationEvent(),this.ready}render(e,o){const i=++this.__lastStartedRenderId,a=Object.assign({search:"",hash:""},Q(e)?{pathname:e}:e,{__renderId:i});return this.ready=this.resolve(a).then(n=>this.__fullyResolveChain(n)).then(n=>{if(this.__isLatestRender(n)){const r=this.__previousContext;if(n===r)return this.__updateBrowserHistory(r,!0),this.location;if(this.location=Y(n),o&&this.__updateBrowserHistory(n,i===1),Ye("location-changed",{router:this,location:this.location}),n.__skipAttach)return this.__copyUnchangedElements(n,r),this.__previousContext=n,this.location;this.__addAppearingContent(n,r);const l=this.__animateIfNeeded(n);return this.__runOnAfterEnterCallbacks(n),this.__runOnAfterLeaveCallbacks(n,r),l.then(()=>{if(this.__isLatestRender(n))return this.__removeDisappearingContent(),this.__previousContext=n,this.location})}}).catch(n=>{if(i===this.__lastStartedRenderId)throw o&&this.__updateBrowserHistory(a),ut(this.__outlet&&this.__outlet.children),this.location=Y(Object.assign(a,{resolver:this})),Ye("error",Object.assign({router:this,error:n},a)),n}),this.ready}__fullyResolveChain(e,o=e){return this.__findComponentContextAfterAllRedirects(o).then(i=>{const n=i!==o?i:e,l=yt(pa(i.chain),i.resolver)===i.pathname,s=(c,d=c.route,h)=>c.next(void 0,d,h).then(m=>m===null||m===Oe?l?c:d.parent!==null?s(c,d.parent,m):m:m);return s(i).then(c=>{if(c===null||c===Oe)throw ia(n);return c&&c!==Oe&&c!==i?this.__fullyResolveChain(n,c):this.__amendWithOnBeforeCallbacks(i)})})}__findComponentContextAfterAllRedirects(e){const o=e.result;return o instanceof HTMLElement?(gn(e,o),Promise.resolve(e)):o.redirect?this.__redirect(o.redirect,e.__redirectCount,e.__renderId).then(i=>this.__findComponentContextAfterAllRedirects(i)):o instanceof Error?Promise.reject(o):Promise.reject(new Error(ee(`Invalid route resolution result for path "${e.pathname}". Expected redirect object or HTML element, but got: "${Da(o)}". Double check the action return value for the route.`)))}__amendWithOnBeforeCallbacks(e){return this.__runOnBeforeCallbacks(e).then(o=>o===this.__previousContext||o===e?o:this.__fullyResolveChain(o))}__runOnBeforeCallbacks(e){const o=this.__previousContext||{},i=o.chain||[],a=e.chain;let n=Promise.resolve();const r=()=>({cancel:!0}),l=s=>xi(e,s);if(e.__divergedChainIndex=0,e.__skipAttach=!1,i.length){for(let s=0;s<Math.min(i.length,a.length)&&!(i[s].route!==a[s].route||i[s].path!==a[s].path&&i[s].element!==a[s].element||!this.__isReusableElement(i[s].element,a[s].element));s=++e.__divergedChainIndex);if(e.__skipAttach=a.length===i.length&&e.__divergedChainIndex==a.length&&this.__isReusableElement(e.result,o.result),e.__skipAttach){for(let s=a.length-1;s>=0;s--)n=this.__runOnBeforeLeaveCallbacks(n,e,{prevent:r},i[s]);for(let s=0;s<a.length;s++)n=this.__runOnBeforeEnterCallbacks(n,e,{prevent:r,redirect:l},a[s]),i[s].element.location=Y(e,i[s].route)}else for(let s=i.length-1;s>=e.__divergedChainIndex;s--)n=this.__runOnBeforeLeaveCallbacks(n,e,{prevent:r},i[s])}if(!e.__skipAttach)for(let s=0;s<a.length;s++)s<e.__divergedChainIndex?s<i.length&&i[s].element&&(i[s].element.location=Y(e,i[s].route)):(n=this.__runOnBeforeEnterCallbacks(n,e,{prevent:r,redirect:l},a[s]),a[s].element&&(a[s].element.location=Y(e,a[s].route)));return n.then(s=>{if(s){if(s.cancel)return this.__previousContext.__renderId=e.__renderId,this.__previousContext;if(s.redirect)return this.__redirect(s.redirect,e.__redirectCount,e.__renderId)}return e})}__runOnBeforeLeaveCallbacks(e,o,i,a){const n=Y(o);return e.then(r=>{if(this.__isLatestRender(o))return yi("onBeforeLeave",[n,i,this],a.element)(r)}).then(r=>{if(!(r||{}).redirect)return r})}__runOnBeforeEnterCallbacks(e,o,i,a){const n=Y(o,a.route);return e.then(r=>{if(this.__isLatestRender(o))return yi("onBeforeEnter",[n,i,this],a.element)(r)})}__isReusableElement(e,o){return e&&o?this.__createdByRouter.get(e)&&this.__createdByRouter.get(o)?e.localName===o.localName:e===o:!1}__isLatestRender(e){return e.__renderId===this.__lastStartedRenderId}__redirect(e,o,i){if(o>un)throw new Error(ee(`Too many redirects when rendering ${e.from}`));return this.resolve({pathname:this.urlForPath(e.pathname,e.params),redirectFrom:e.from,__redirectCount:(o||0)+1,__renderId:i})}__ensureOutlet(e=this.__outlet){if(!(e instanceof Node))throw new TypeError(ee(`Expected router outlet to be a valid DOM Node (but got ${e})`))}__updateBrowserHistory({pathname:e,search:o="",hash:i=""},a){if(window.location.pathname!==e||window.location.search!==o||window.location.hash!==i){const n=a?"replaceState":"pushState";window.history[n](null,document.title,e+o+i),window.dispatchEvent(new PopStateEvent("popstate",{state:"vaadin-router-ignore"}))}}__copyUnchangedElements(e,o){let i=this.__outlet;for(let a=0;a<e.__divergedChainIndex;a++){const n=o&&o.chain[a].element;if(n)if(n.parentNode===i)e.chain[a].element=n,i=n;else break}return i}__addAppearingContent(e,o){this.__ensureOutlet(),this.__removeAppearingContent();const i=this.__copyUnchangedElements(e,o);this.__appearingContent=[],this.__disappearingContent=Array.from(i.children).filter(n=>this.__addedByRouter.get(n)&&n!==e.result);let a=i;for(let n=e.__divergedChainIndex;n<e.chain.length;n++){const r=e.chain[n].element;r&&(a.appendChild(r),this.__addedByRouter.set(r,!0),a===i&&this.__appearingContent.push(r),a=r)}}__removeDisappearingContent(){this.__disappearingContent&&ut(this.__disappearingContent),this.__disappearingContent=null,this.__appearingContent=null}__removeAppearingContent(){this.__disappearingContent&&this.__appearingContent&&(ut(this.__appearingContent),this.__disappearingContent=null,this.__appearingContent=null)}__runOnAfterLeaveCallbacks(e,o){if(o)for(let i=o.chain.length-1;i>=e.__divergedChainIndex&&this.__isLatestRender(e);i--){const a=o.chain[i].element;if(a)try{const n=Y(e);xt(a.onAfterLeave,[n,{},o.resolver],a)}finally{this.__disappearingContent.indexOf(a)>-1&&ut(a.children)}}}__runOnAfterEnterCallbacks(e){for(let o=e.__divergedChainIndex;o<e.chain.length&&this.__isLatestRender(e);o++){const i=e.chain[o].element||{},a=Y(e,e.chain[o].route);xt(i.onAfterEnter,[a,{},e.resolver],i)}}__animateIfNeeded(e){const o=(this.__disappearingContent||[])[0],i=(this.__appearingContent||[])[0],a=[],n=e.chain;let r;for(let l=n.length;l>0;l--)if(n[l-1].route.animate){r=n[l-1].route.animate;break}if(o&&i&&r){const l=zt(r)&&r.leave||"leaving",s=zt(r)&&r.enter||"entering";a.push(vi(o,l)),a.push(vi(i,s))}return Promise.all(a).then(()=>e)}subscribe(){window.addEventListener("vaadin-router-go",this.__navigationEventHandler)}unsubscribe(){window.removeEventListener("vaadin-router-go",this.__navigationEventHandler)}__onNavigationEvent(e){const{pathname:o,search:i,hash:a}=e?e.detail:window.location;Q(this.__normalizePathname(o))&&(e&&e.preventDefault&&e.preventDefault(),this.render({pathname:o,search:i,hash:a},!0))}static setTriggers(...e){dn(e)}urlForName(e,o){return this.__urlForName||(this.__urlForName=cn(this)),yt(this.__urlForName(e,o),this)}urlForPath(e,o){return yt(ge.pathToRegexp.compile(e)(o),this)}static go(e){const{pathname:o,search:i,hash:a}=Q(e)?this.__createUrl(e,"http://a"):e;return Ye("go",{pathname:o,search:i,hash:a})}}const vn=/\/\*[\*!]\s+vaadin-dev-mode:start([\s\S]*)vaadin-dev-mode:end\s+\*\*\//i,bt=window.Vaadin&&window.Vaadin.Flow&&window.Vaadin.Flow.clients;function xn(){function t(){return!0}return ua(t)}function yn(){try{return bn()?!0:wn()?bt?!_n():!xn():!1}catch{return!1}}function bn(){return localStorage.getItem("vaadin.developmentmode.force")}function wn(){return["localhost","127.0.0.1"].indexOf(window.location.hostname)>=0}function _n(){return!!(bt&&Object.keys(bt).map(e=>bt[e]).filter(e=>e.productionMode).length>0)}function ua(t,e){if(typeof t!="function")return;const o=vn.exec(t.toString());if(o)try{t=new Function(o[1])}catch(i){console.log("vaadin-development-mode-detector: uncommentAndRun() failed",i)}return t(e)}window.Vaadin=window.Vaadin||{};const bi=function(t,e){if(window.Vaadin.developmentMode)return ua(t,e)};window.Vaadin.developmentMode===void 0&&(window.Vaadin.developmentMode=yn());function kn(){}const En=function(){if(typeof bi=="function")return bi(kn)};window.Vaadin=window.Vaadin||{};window.Vaadin.registrations=window.Vaadin.registrations||[];window.Vaadin.registrations.push({is:"@vaadin/router",version:"1.7.4"});En();ge.NavigationTrigger={POPSTATE:Ha,CLICK:Fa};var Kt,R;(function(t){t.CONNECTED="connected",t.LOADING="loading",t.RECONNECTING="reconnecting",t.CONNECTION_LOST="connection-lost"})(R||(R={}));class Sn{constructor(e){this.stateChangeListeners=new Set,this.loadingCount=0,this.connectionState=e,this.serviceWorkerMessageListener=this.serviceWorkerMessageListener.bind(this),navigator.serviceWorker&&(navigator.serviceWorker.addEventListener("message",this.serviceWorkerMessageListener),navigator.serviceWorker.ready.then(o=>{var i;(i=o==null?void 0:o.active)===null||i===void 0||i.postMessage({method:"Vaadin.ServiceWorker.isConnectionLost",id:"Vaadin.ServiceWorker.isConnectionLost"})}))}addStateChangeListener(e){this.stateChangeListeners.add(e)}removeStateChangeListener(e){this.stateChangeListeners.delete(e)}loadingStarted(){this.state=R.LOADING,this.loadingCount+=1}loadingFinished(){this.decreaseLoadingCount(R.CONNECTED)}loadingFailed(){this.decreaseLoadingCount(R.CONNECTION_LOST)}decreaseLoadingCount(e){this.loadingCount>0&&(this.loadingCount-=1,this.loadingCount===0&&(this.state=e))}get state(){return this.connectionState}set state(e){if(e!==this.connectionState){const o=this.connectionState;this.connectionState=e,this.loadingCount=0;for(const i of this.stateChangeListeners)i(o,this.connectionState)}}get online(){return this.connectionState===R.CONNECTED||this.connectionState===R.LOADING}get offline(){return!this.online}serviceWorkerMessageListener(e){typeof e.data=="object"&&e.data.id==="Vaadin.ServiceWorker.isConnectionLost"&&(e.data.result===!0&&(this.state=R.CONNECTION_LOST),navigator.serviceWorker.removeEventListener("message",this.serviceWorkerMessageListener))}}const Cn=t=>!!(t==="localhost"||t==="[::1]"||t.match(/^127\.\d+\.\d+\.\d+$/)),ht=window;if(!(!((Kt=ht.Vaadin)===null||Kt===void 0)&&Kt.connectionState)){let t;Cn(window.location.hostname)?t=!0:t=navigator.onLine,ht.Vaadin=ht.Vaadin||{},ht.Vaadin.connectionState=new Sn(t?R.CONNECTED:R.CONNECTION_LOST)}function H(t,e,o,i){var a=arguments.length,n=a<3?e:i===null?i=Object.getOwnPropertyDescriptor(e,o):i,r;if(typeof Reflect=="object"&&typeof Reflect.decorate=="function")n=Reflect.decorate(t,e,o,i);else for(var l=t.length-1;l>=0;l--)(r=t[l])&&(n=(a<3?r(n):a>3?r(e,o,n):r(e,o))||n);return a>3&&n&&Object.defineProperty(e,o,n),n}/**
 * @license
 * Copyright 2019 Google LLC
 * SPDX-License-Identifier: BSD-3-Clause
 */const zn=!1,wt=window,Oo=wt.ShadowRoot&&(wt.ShadyCSS===void 0||wt.ShadyCSS.nativeShadow)&&"adoptedStyleSheets"in Document.prototype&&"replace"in CSSStyleSheet.prototype,Lo=Symbol(),wi=new WeakMap;class Mo{constructor(e,o,i){if(this._$cssResult$=!0,i!==Lo)throw new Error("CSSResult is not constructable. Use `unsafeCSS` or `css` instead.");this.cssText=e,this._strings=o}get styleSheet(){let e=this._styleSheet;const o=this._strings;if(Oo&&e===void 0){const i=o!==void 0&&o.length===1;i&&(e=wi.get(o)),e===void 0&&((this._styleSheet=e=new CSSStyleSheet).replaceSync(this.cssText),i&&wi.set(o,e))}return e}toString(){return this.cssText}}const Tn=t=>{if(t._$cssResult$===!0)return t.cssText;if(typeof t=="number")return t;throw new Error(`Value passed to 'css' function must be a 'css' function result: ${t}. Use 'unsafeCSS' to pass non-literal values, but take care to ensure page security.`)},A=t=>new Mo(typeof t=="string"?t:String(t),void 0,Lo),k=(t,...e)=>{const o=t.length===1?t[0]:e.reduce((i,a,n)=>i+Tn(a)+t[n+1],t[0]);return new Mo(o,t,Lo)},$n=(t,e)=>{Oo?t.adoptedStyleSheets=e.map(o=>o instanceof CSSStyleSheet?o:o.styleSheet):e.forEach(o=>{const i=document.createElement("style"),a=wt.litNonce;a!==void 0&&i.setAttribute("nonce",a),i.textContent=o.cssText,t.appendChild(i)})},jn=t=>{let e="";for(const o of t.cssRules)e+=o.cssText;return A(e)},_i=Oo||zn?t=>t:t=>t instanceof CSSStyleSheet?jn(t):t;/**
 * @license
 * Copyright 2017 Google LLC
 * SPDX-License-Identifier: BSD-3-Clause
 */var Yt,Jt,Xt,ha;const ae=window;let ga,fe;const ki=ae.trustedTypes,An=ki?ki.emptyScript:"",_t=ae.reactiveElementPolyfillSupportDevMode;{const t=(Yt=ae.litIssuedWarnings)!==null&&Yt!==void 0?Yt:ae.litIssuedWarnings=new Set;fe=(e,o)=>{o+=` See https://lit.dev/msg/${e} for more information.`,t.has(o)||(console.warn(o),t.add(o))},fe("dev-mode","Lit is in dev mode. Not recommended for production!"),!((Jt=ae.ShadyDOM)===null||Jt===void 0)&&Jt.inUse&&_t===void 0&&fe("polyfill-support-missing","Shadow DOM is being polyfilled via `ShadyDOM` but the `polyfill-support` module has not been loaded."),ga=e=>({then:(o,i)=>{fe("request-update-promise",`The \`requestUpdate\` method should no longer return a Promise but does so on \`${e}\`. Use \`updateComplete\` instead.`),o!==void 0&&o(!1)}})}const Qt=t=>{ae.emitLitDebugLogEvents&&ae.dispatchEvent(new CustomEvent("lit-debug",{detail:t}))},fa=(t,e)=>t,Eo={toAttribute(t,e){switch(e){case Boolean:t=t?An:null;break;case Object:case Array:t=t==null?t:JSON.stringify(t);break}return t},fromAttribute(t,e){let o=t;switch(e){case Boolean:o=t!==null;break;case Number:o=t===null?null:Number(t);break;case Object:case Array:try{o=JSON.parse(t)}catch{o=null}break}return o}},va=(t,e)=>e!==t&&(e===e||t===t),Zt={attribute:!0,type:String,converter:Eo,reflect:!1,hasChanged:va},So="finalized";class ne extends HTMLElement{constructor(){super(),this.__instanceProperties=new Map,this.isUpdatePending=!1,this.hasUpdated=!1,this.__reflectingProperty=null,this.__initialize()}static addInitializer(e){var o;this.finalize(),((o=this._initializers)!==null&&o!==void 0?o:this._initializers=[]).push(e)}static get observedAttributes(){this.finalize();const e=[];return this.elementProperties.forEach((o,i)=>{const a=this.__attributeNameForProperty(i,o);a!==void 0&&(this.__attributeToPropertyMap.set(a,i),e.push(a))}),e}static createProperty(e,o=Zt){var i;if(o.state&&(o.attribute=!1),this.finalize(),this.elementProperties.set(e,o),!o.noAccessor&&!this.prototype.hasOwnProperty(e)){const a=typeof e=="symbol"?Symbol():`__${e}`,n=this.getPropertyDescriptor(e,a,o);n!==void 0&&(Object.defineProperty(this.prototype,e,n),this.hasOwnProperty("__reactivePropertyKeys")||(this.__reactivePropertyKeys=new Set((i=this.__reactivePropertyKeys)!==null&&i!==void 0?i:[])),this.__reactivePropertyKeys.add(e))}}static getPropertyDescriptor(e,o,i){return{get(){return this[o]},set(a){const n=this[e];this[o]=a,this.requestUpdate(e,n,i)},configurable:!0,enumerable:!0}}static getPropertyOptions(e){return this.elementProperties.get(e)||Zt}static finalize(){if(this.hasOwnProperty(So))return!1;this[So]=!0;const e=Object.getPrototypeOf(this);if(e.finalize(),e._initializers!==void 0&&(this._initializers=[...e._initializers]),this.elementProperties=new Map(e.elementProperties),this.__attributeToPropertyMap=new Map,this.hasOwnProperty(fa("properties"))){const o=this.properties,i=[...Object.getOwnPropertyNames(o),...Object.getOwnPropertySymbols(o)];for(const a of i)this.createProperty(a,o[a])}this.elementStyles=this.finalizeStyles(this.styles);{const o=(i,a=!1)=>{this.prototype.hasOwnProperty(i)&&fe(a?"renamed-api":"removed-api",`\`${i}\` is implemented on class ${this.name}. It has been ${a?"renamed":"removed"} in this version of LitElement.`)};o("initialize"),o("requestUpdateInternal"),o("_getUpdateComplete",!0)}return!0}static finalizeStyles(e){const o=[];if(Array.isArray(e)){const i=new Set(e.flat(1/0).reverse());for(const a of i)o.unshift(_i(a))}else e!==void 0&&o.push(_i(e));return o}static __attributeNameForProperty(e,o){const i=o.attribute;return i===!1?void 0:typeof i=="string"?i:typeof e=="string"?e.toLowerCase():void 0}__initialize(){var e;this.__updatePromise=new Promise(o=>this.enableUpdating=o),this._$changedProperties=new Map,this.__saveInstanceProperties(),this.requestUpdate(),(e=this.constructor._initializers)===null||e===void 0||e.forEach(o=>o(this))}addController(e){var o,i;((o=this.__controllers)!==null&&o!==void 0?o:this.__controllers=[]).push(e),this.renderRoot!==void 0&&this.isConnected&&((i=e.hostConnected)===null||i===void 0||i.call(e))}removeController(e){var o;(o=this.__controllers)===null||o===void 0||o.splice(this.__controllers.indexOf(e)>>>0,1)}__saveInstanceProperties(){this.constructor.elementProperties.forEach((e,o)=>{this.hasOwnProperty(o)&&(this.__instanceProperties.set(o,this[o]),delete this[o])})}createRenderRoot(){var e;const o=(e=this.shadowRoot)!==null&&e!==void 0?e:this.attachShadow(this.constructor.shadowRootOptions);return $n(o,this.constructor.elementStyles),o}connectedCallback(){var e;this.renderRoot===void 0&&(this.renderRoot=this.createRenderRoot()),this.enableUpdating(!0),(e=this.__controllers)===null||e===void 0||e.forEach(o=>{var i;return(i=o.hostConnected)===null||i===void 0?void 0:i.call(o)})}enableUpdating(e){}disconnectedCallback(){var e;(e=this.__controllers)===null||e===void 0||e.forEach(o=>{var i;return(i=o.hostDisconnected)===null||i===void 0?void 0:i.call(o)})}attributeChangedCallback(e,o,i){this._$attributeToProperty(e,i)}__propertyToAttribute(e,o,i=Zt){var a;const n=this.constructor.__attributeNameForProperty(e,i);if(n!==void 0&&i.reflect===!0){const l=(((a=i.converter)===null||a===void 0?void 0:a.toAttribute)!==void 0?i.converter:Eo).toAttribute(o,i.type);this.constructor.enabledWarnings.indexOf("migration")>=0&&l===void 0&&fe("undefined-attribute-value",`The attribute value for the ${e} property is undefined on element ${this.localName}. The attribute will be removed, but in the previous version of \`ReactiveElement\`, the attribute would not have changed.`),this.__reflectingProperty=e,l==null?this.removeAttribute(n):this.setAttribute(n,l),this.__reflectingProperty=null}}_$attributeToProperty(e,o){var i;const a=this.constructor,n=a.__attributeToPropertyMap.get(e);if(n!==void 0&&this.__reflectingProperty!==n){const r=a.getPropertyOptions(n),l=typeof r.converter=="function"?{fromAttribute:r.converter}:((i=r.converter)===null||i===void 0?void 0:i.fromAttribute)!==void 0?r.converter:Eo;this.__reflectingProperty=n,this[n]=l.fromAttribute(o,r.type),this.__reflectingProperty=null}}requestUpdate(e,o,i){let a=!0;return e!==void 0&&(i=i||this.constructor.getPropertyOptions(e),(i.hasChanged||va)(this[e],o)?(this._$changedProperties.has(e)||this._$changedProperties.set(e,o),i.reflect===!0&&this.__reflectingProperty!==e&&(this.__reflectingProperties===void 0&&(this.__reflectingProperties=new Map),this.__reflectingProperties.set(e,i))):a=!1),!this.isUpdatePending&&a&&(this.__updatePromise=this.__enqueueUpdate()),ga(this.localName)}async __enqueueUpdate(){this.isUpdatePending=!0;try{await this.__updatePromise}catch(o){Promise.reject(o)}const e=this.scheduleUpdate();return e!=null&&await e,!this.isUpdatePending}scheduleUpdate(){return this.performUpdate()}performUpdate(){var e,o;if(!this.isUpdatePending)return;if(Qt==null||Qt({kind:"update"}),!this.hasUpdated){const n=[];if((e=this.constructor.__reactivePropertyKeys)===null||e===void 0||e.forEach(r=>{var l;this.hasOwnProperty(r)&&!(!((l=this.__instanceProperties)===null||l===void 0)&&l.has(r))&&n.push(r)}),n.length)throw new Error(`The following properties on element ${this.localName} will not trigger updates as expected because they are set using class fields: ${n.join(", ")}. Native class fields and some compiled output will overwrite accessors used for detecting changes. See https://lit.dev/msg/class-field-shadowing for more information.`)}this.__instanceProperties&&(this.__instanceProperties.forEach((n,r)=>this[r]=n),this.__instanceProperties=void 0);let i=!1;const a=this._$changedProperties;try{i=this.shouldUpdate(a),i?(this.willUpdate(a),(o=this.__controllers)===null||o===void 0||o.forEach(n=>{var r;return(r=n.hostUpdate)===null||r===void 0?void 0:r.call(n)}),this.update(a)):this.__markUpdated()}catch(n){throw i=!1,this.__markUpdated(),n}i&&this._$didUpdate(a)}willUpdate(e){}_$didUpdate(e){var o;(o=this.__controllers)===null||o===void 0||o.forEach(i=>{var a;return(a=i.hostUpdated)===null||a===void 0?void 0:a.call(i)}),this.hasUpdated||(this.hasUpdated=!0,this.firstUpdated(e)),this.updated(e),this.isUpdatePending&&this.constructor.enabledWarnings.indexOf("change-in-update")>=0&&fe("change-in-update",`Element ${this.localName} scheduled an update (generally because a property was set) after an update completed, causing a new update to be scheduled. This is inefficient and should be avoided unless the next update can only be scheduled as a side effect of the previous update.`)}__markUpdated(){this._$changedProperties=new Map,this.isUpdatePending=!1}get updateComplete(){return this.getUpdateComplete()}getUpdateComplete(){return this.__updatePromise}shouldUpdate(e){return!0}update(e){this.__reflectingProperties!==void 0&&(this.__reflectingProperties.forEach((o,i)=>this.__propertyToAttribute(i,this[i],o)),this.__reflectingProperties=void 0),this.__markUpdated()}updated(e){}firstUpdated(e){}}ha=So;ne[ha]=!0;ne.elementProperties=new Map;ne.elementStyles=[];ne.shadowRootOptions={mode:"open"};_t==null||_t({ReactiveElement:ne});{ne.enabledWarnings=["change-in-update"];const t=function(e){e.hasOwnProperty(fa("enabledWarnings"))||(e.enabledWarnings=e.enabledWarnings.slice())};ne.enableWarning=function(e){t(this),this.enabledWarnings.indexOf(e)<0&&this.enabledWarnings.push(e)},ne.disableWarning=function(e){t(this);const o=this.enabledWarnings.indexOf(e);o>=0&&this.enabledWarnings.splice(o,1)}}((Xt=ae.reactiveElementVersions)!==null&&Xt!==void 0?Xt:ae.reactiveElementVersions=[]).push("1.6.3");ae.reactiveElementVersions.length>1&&fe("multiple-versions","Multiple versions of Lit loaded. Loading multiple versions is not recommended.");/**
 * @license
 * Copyright 2017 Google LLC
 * SPDX-License-Identifier: BSD-3-Clause
 */var eo,to,oo,io;const B=window,b=t=>{B.emitLitDebugLogEvents&&B.dispatchEvent(new CustomEvent("lit-debug",{detail:t}))};let Rn=0,Tt;(eo=B.litIssuedWarnings)!==null&&eo!==void 0||(B.litIssuedWarnings=new Set),Tt=(t,e)=>{e+=t?` See https://lit.dev/msg/${t} for more information.`:"",B.litIssuedWarnings.has(e)||(console.warn(e),B.litIssuedWarnings.add(e))},Tt("dev-mode","Lit is in dev mode. Not recommended for production!");const G=!((to=B.ShadyDOM)===null||to===void 0)&&to.inUse&&((oo=B.ShadyDOM)===null||oo===void 0?void 0:oo.noPatch)===!0?B.ShadyDOM.wrap:t=>t,Ve=B.trustedTypes,Ei=Ve?Ve.createPolicy("lit-html",{createHTML:t=>t}):void 0,Nn=t=>t,Dt=(t,e,o)=>Nn,In=t=>{if(Te!==Dt)throw new Error("Attempted to overwrite existing lit-html security policy. setSanitizeDOMValueFactory should be called at most once.");Te=t},Pn=()=>{Te=Dt},Co=(t,e,o)=>Te(t,e,o),zo="$lit$",le=`lit$${String(Math.random()).slice(9)}$`,xa="?"+le,On=`<${xa}>`,Ce=document,Qe=()=>Ce.createComment(""),Ze=t=>t===null||typeof t!="object"&&typeof t!="function",ya=Array.isArray,Ln=t=>ya(t)||typeof(t==null?void 0:t[Symbol.iterator])=="function",ao=`[ 	
\f\r]`,Mn=`[^ 	
\f\r"'\`<>=]`,Vn=`[^\\s"'>=/]`,We=/<(?:(!--|\/[^a-zA-Z])|(\/?[a-zA-Z][^>\s]*)|(\/?$))/g,Si=1,no=2,Dn=3,Ci=/-->/g,zi=/>/g,be=new RegExp(`>|${ao}(?:(${Vn}+)(${ao}*=${ao}*(?:${Mn}|("|')|))|$)`,"g"),Un=0,Ti=1,qn=2,$i=3,ro=/'/g,so=/"/g,ba=/^(?:script|style|textarea|title)$/i,Fn=1,$t=2,Vo=1,jt=2,Bn=3,Hn=4,Wn=5,Do=6,Gn=7,wa=t=>(e,...o)=>(e.some(i=>i===void 0)&&console.warn(`Some template strings are undefined.
This is probably caused by illegal octal escape sequences.`),{_$litType$:t,strings:e,values:o}),y=wa(Fn),Pe=wa($t),ze=Symbol.for("lit-noChange"),$=Symbol.for("lit-nothing"),ji=new WeakMap,ke=Ce.createTreeWalker(Ce,129,null,!1);let Te=Dt;function _a(t,e){if(!Array.isArray(t)||!t.hasOwnProperty("raw")){let o="invalid template strings array";throw o=`
          Internal Error: expected template strings to be an array
          with a 'raw' field. Faking a template strings array by
          calling html or svg like an ordinary function is effectively
          the same as calling unsafeHtml and can lead to major security
          issues, e.g. opening your code up to XSS attacks.
          If you're using the html or svg tagged template functions normally
          and still seeing this error, please file a bug at
          https://github.com/lit/lit/issues/new?template=bug_report.md
          and include information about your build tooling, if any.
        `.trim().replace(/\n */g,`
`),new Error(o)}return Ei!==void 0?Ei.createHTML(e):e}const Kn=(t,e)=>{const o=t.length-1,i=[];let a=e===$t?"<svg>":"",n,r=We;for(let s=0;s<o;s++){const c=t[s];let d=-1,h,m=0,x;for(;m<c.length&&(r.lastIndex=m,x=r.exec(c),x!==null);)if(m=r.lastIndex,r===We){if(x[Si]==="!--")r=Ci;else if(x[Si]!==void 0)r=zi;else if(x[no]!==void 0)ba.test(x[no])&&(n=new RegExp(`</${x[no]}`,"g")),r=be;else if(x[Dn]!==void 0)throw new Error("Bindings in tag names are not supported. Please use static templates instead. See https://lit.dev/docs/templates/expressions/#static-expressions")}else r===be?x[Un]===">"?(r=n??We,d=-1):x[Ti]===void 0?d=-2:(d=r.lastIndex-x[qn].length,h=x[Ti],r=x[$i]===void 0?be:x[$i]==='"'?so:ro):r===so||r===ro?r=be:r===Ci||r===zi?r=We:(r=be,n=void 0);console.assert(d===-1||r===be||r===ro||r===so,"unexpected parse state B");const pe=r===be&&t[s+1].startsWith("/>")?" ":"";a+=r===We?c+On:d>=0?(i.push(h),c.slice(0,d)+zo+c.slice(d)+le+pe):c+le+(d===-2?(i.push(void 0),s):pe)}const l=a+(t[o]||"<?>")+(e===$t?"</svg>":"");return[_a(t,l),i]};class et{constructor({strings:e,["_$litType$"]:o},i){this.parts=[];let a,n=0,r=0;const l=e.length-1,s=this.parts,[c,d]=Kn(e,o);if(this.el=et.createElement(c,i),ke.currentNode=this.el.content,o===$t){const h=this.el.content,m=h.firstChild;m.remove(),h.append(...m.childNodes)}for(;(a=ke.nextNode())!==null&&s.length<l;){if(a.nodeType===1){{const h=a.localName;if(/^(?:textarea|template)$/i.test(h)&&a.innerHTML.includes(le)){const m=`Expressions are not supported inside \`${h}\` elements. See https://lit.dev/msg/expression-in-${h} for more information.`;if(h==="template")throw new Error(m);Tt("",m)}}if(a.hasAttributes()){const h=[];for(const m of a.getAttributeNames())if(m.endsWith(zo)||m.startsWith(le)){const x=d[r++];if(h.push(m),x!==void 0){const ue=a.getAttribute(x.toLowerCase()+zo).split(le),re=/([.?@])?(.*)/.exec(x);s.push({type:Vo,index:n,name:re[2],strings:ue,ctor:re[1]==="."?Jn:re[1]==="?"?Qn:re[1]==="@"?Zn:Ut})}else s.push({type:Do,index:n})}for(const m of h)a.removeAttribute(m)}if(ba.test(a.tagName)){const h=a.textContent.split(le),m=h.length-1;if(m>0){a.textContent=Ve?Ve.emptyScript:"";for(let x=0;x<m;x++)a.append(h[x],Qe()),ke.nextNode(),s.push({type:jt,index:++n});a.append(h[m],Qe())}}}else if(a.nodeType===8)if(a.data===xa)s.push({type:jt,index:n});else{let m=-1;for(;(m=a.data.indexOf(le,m+1))!==-1;)s.push({type:Gn,index:n}),m+=le.length-1}n++}b==null||b({kind:"template prep",template:this,clonableTemplate:this.el,parts:this.parts,strings:e})}static createElement(e,o){const i=Ce.createElement("template");return i.innerHTML=e,i}}function De(t,e,o=t,i){var a,n,r,l;if(e===ze)return e;let s=i!==void 0?(a=o.__directives)===null||a===void 0?void 0:a[i]:o.__directive;const c=Ze(e)?void 0:e._$litDirective$;return(s==null?void 0:s.constructor)!==c&&((n=s==null?void 0:s._$notifyDirectiveConnectionChanged)===null||n===void 0||n.call(s,!1),c===void 0?s=void 0:(s=new c(t),s._$initialize(t,o,i)),i!==void 0?((r=(l=o).__directives)!==null&&r!==void 0?r:l.__directives=[])[i]=s:o.__directive=s),s!==void 0&&(e=De(t,s._$resolve(t,e.values),s,i)),e}class Yn{constructor(e,o){this._$parts=[],this._$disconnectableChildren=void 0,this._$template=e,this._$parent=o}get parentNode(){return this._$parent.parentNode}get _$isConnected(){return this._$parent._$isConnected}_clone(e){var o;const{el:{content:i},parts:a}=this._$template,n=((o=e==null?void 0:e.creationScope)!==null&&o!==void 0?o:Ce).importNode(i,!0);ke.currentNode=n;let r=ke.nextNode(),l=0,s=0,c=a[0];for(;c!==void 0;){if(l===c.index){let d;c.type===jt?d=new rt(r,r.nextSibling,this,e):c.type===Vo?d=new c.ctor(r,c.name,c.strings,this,e):c.type===Do&&(d=new er(r,this,e)),this._$parts.push(d),c=a[++s]}l!==(c==null?void 0:c.index)&&(r=ke.nextNode(),l++)}return ke.currentNode=Ce,n}_update(e){let o=0;for(const i of this._$parts)i!==void 0&&(b==null||b({kind:"set part",part:i,value:e[o],valueIndex:o,values:e,templateInstance:this}),i.strings!==void 0?(i._$setValue(e,i,o),o+=i.strings.length-2):i._$setValue(e[o])),o++}}class rt{constructor(e,o,i,a){var n;this.type=jt,this._$committedValue=$,this._$disconnectableChildren=void 0,this._$startNode=e,this._$endNode=o,this._$parent=i,this.options=a,this.__isConnected=(n=a==null?void 0:a.isConnected)!==null&&n!==void 0?n:!0,this._textSanitizer=void 0}get _$isConnected(){var e,o;return(o=(e=this._$parent)===null||e===void 0?void 0:e._$isConnected)!==null&&o!==void 0?o:this.__isConnected}get parentNode(){let e=G(this._$startNode).parentNode;const o=this._$parent;return o!==void 0&&(e==null?void 0:e.nodeType)===11&&(e=o.parentNode),e}get startNode(){return this._$startNode}get endNode(){return this._$endNode}_$setValue(e,o=this){var i;if(this.parentNode===null)throw new Error("This `ChildPart` has no `parentNode` and therefore cannot accept a value. This likely means the element containing the part was manipulated in an unsupported way outside of Lit's control such that the part's marker nodes were ejected from DOM. For example, setting the element's `innerHTML` or `textContent` can do this.");if(e=De(this,e,o),Ze(e))e===$||e==null||e===""?(this._$committedValue!==$&&(b==null||b({kind:"commit nothing to child",start:this._$startNode,end:this._$endNode,parent:this._$parent,options:this.options}),this._$clear()),this._$committedValue=$):e!==this._$committedValue&&e!==ze&&this._commitText(e);else if(e._$litType$!==void 0)this._commitTemplateResult(e);else if(e.nodeType!==void 0){if(((i=this.options)===null||i===void 0?void 0:i.host)===e){this._commitText("[probable mistake: rendered a template's host in itself (commonly caused by writing ${this} in a template]"),console.warn("Attempted to render the template host",e,"inside itself. This is almost always a mistake, and in dev mode ","we render some warning text. In production however, we'll ","render it, which will usually result in an error, and sometimes ","in the element disappearing from the DOM.");return}this._commitNode(e)}else Ln(e)?this._commitIterable(e):this._commitText(e)}_insert(e){return G(G(this._$startNode).parentNode).insertBefore(e,this._$endNode)}_commitNode(e){var o;if(this._$committedValue!==e){if(this._$clear(),Te!==Dt){const i=(o=this._$startNode.parentNode)===null||o===void 0?void 0:o.nodeName;if(i==="STYLE"||i==="SCRIPT"){let a="Forbidden";throw i==="STYLE"?a="Lit does not support binding inside style nodes. This is a security risk, as style injection attacks can exfiltrate data and spoof UIs. Consider instead using css`...` literals to compose styles, and make do dynamic styling with css custom properties, ::parts, <slot>s, and by mutating the DOM rather than stylesheets.":a="Lit does not support binding inside script nodes. This is a security risk, as it could allow arbitrary code execution.",new Error(a)}}b==null||b({kind:"commit node",start:this._$startNode,parent:this._$parent,value:e,options:this.options}),this._$committedValue=this._insert(e)}}_commitText(e){if(this._$committedValue!==$&&Ze(this._$committedValue)){const o=G(this._$startNode).nextSibling;this._textSanitizer===void 0&&(this._textSanitizer=Co(o,"data","property")),e=this._textSanitizer(e),b==null||b({kind:"commit text",node:o,value:e,options:this.options}),o.data=e}else{const o=Ce.createTextNode("");this._commitNode(o),this._textSanitizer===void 0&&(this._textSanitizer=Co(o,"data","property")),e=this._textSanitizer(e),b==null||b({kind:"commit text",node:o,value:e,options:this.options}),o.data=e}this._$committedValue=e}_commitTemplateResult(e){var o;const{values:i,["_$litType$"]:a}=e,n=typeof a=="number"?this._$getTemplate(e):(a.el===void 0&&(a.el=et.createElement(_a(a.h,a.h[0]),this.options)),a);if(((o=this._$committedValue)===null||o===void 0?void 0:o._$template)===n)b==null||b({kind:"template updating",template:n,instance:this._$committedValue,parts:this._$committedValue._$parts,options:this.options,values:i}),this._$committedValue._update(i);else{const r=new Yn(n,this),l=r._clone(this.options);b==null||b({kind:"template instantiated",template:n,instance:r,parts:r._$parts,options:this.options,fragment:l,values:i}),r._update(i),b==null||b({kind:"template instantiated and updated",template:n,instance:r,parts:r._$parts,options:this.options,fragment:l,values:i}),this._commitNode(l),this._$committedValue=r}}_$getTemplate(e){let o=ji.get(e.strings);return o===void 0&&ji.set(e.strings,o=new et(e)),o}_commitIterable(e){ya(this._$committedValue)||(this._$committedValue=[],this._$clear());const o=this._$committedValue;let i=0,a;for(const n of e)i===o.length?o.push(a=new rt(this._insert(Qe()),this._insert(Qe()),this,this.options)):a=o[i],a._$setValue(n),i++;i<o.length&&(this._$clear(a&&G(a._$endNode).nextSibling,i),o.length=i)}_$clear(e=G(this._$startNode).nextSibling,o){var i;for((i=this._$notifyConnectionChanged)===null||i===void 0||i.call(this,!1,!0,o);e&&e!==this._$endNode;){const a=G(e).nextSibling;G(e).remove(),e=a}}setConnected(e){var o;if(this._$parent===void 0)this.__isConnected=e,(o=this._$notifyConnectionChanged)===null||o===void 0||o.call(this,e);else throw new Error("part.setConnected() may only be called on a RootPart returned from render().")}}class Ut{constructor(e,o,i,a,n){this.type=Vo,this._$committedValue=$,this._$disconnectableChildren=void 0,this.element=e,this.name=o,this._$parent=a,this.options=n,i.length>2||i[0]!==""||i[1]!==""?(this._$committedValue=new Array(i.length-1).fill(new String),this.strings=i):this._$committedValue=$,this._sanitizer=void 0}get tagName(){return this.element.tagName}get _$isConnected(){return this._$parent._$isConnected}_$setValue(e,o=this,i,a){const n=this.strings;let r=!1;if(n===void 0)e=De(this,e,o,0),r=!Ze(e)||e!==this._$committedValue&&e!==ze,r&&(this._$committedValue=e);else{const l=e;e=n[0];let s,c;for(s=0;s<n.length-1;s++)c=De(this,l[i+s],o,s),c===ze&&(c=this._$committedValue[s]),r||(r=!Ze(c)||c!==this._$committedValue[s]),c===$?e=$:e!==$&&(e+=(c??"")+n[s+1]),this._$committedValue[s]=c}r&&!a&&this._commitValue(e)}_commitValue(e){e===$?G(this.element).removeAttribute(this.name):(this._sanitizer===void 0&&(this._sanitizer=Te(this.element,this.name,"attribute")),e=this._sanitizer(e??""),b==null||b({kind:"commit attribute",element:this.element,name:this.name,value:e,options:this.options}),G(this.element).setAttribute(this.name,e??""))}}class Jn extends Ut{constructor(){super(...arguments),this.type=Bn}_commitValue(e){this._sanitizer===void 0&&(this._sanitizer=Te(this.element,this.name,"property")),e=this._sanitizer(e),b==null||b({kind:"commit property",element:this.element,name:this.name,value:e,options:this.options}),this.element[this.name]=e===$?void 0:e}}const Xn=Ve?Ve.emptyScript:"";class Qn extends Ut{constructor(){super(...arguments),this.type=Hn}_commitValue(e){b==null||b({kind:"commit boolean attribute",element:this.element,name:this.name,value:!!(e&&e!==$),options:this.options}),e&&e!==$?G(this.element).setAttribute(this.name,Xn):G(this.element).removeAttribute(this.name)}}class Zn extends Ut{constructor(e,o,i,a,n){if(super(e,o,i,a,n),this.type=Wn,this.strings!==void 0)throw new Error(`A \`<${e.localName}>\` has a \`@${o}=...\` listener with invalid content. Event listeners in templates must have exactly one expression and no surrounding text.`)}_$setValue(e,o=this){var i;if(e=(i=De(this,e,o,0))!==null&&i!==void 0?i:$,e===ze)return;const a=this._$committedValue,n=e===$&&a!==$||e.capture!==a.capture||e.once!==a.once||e.passive!==a.passive,r=e!==$&&(a===$||n);b==null||b({kind:"commit event listener",element:this.element,name:this.name,value:e,options:this.options,removeListener:n,addListener:r,oldListener:a}),n&&this.element.removeEventListener(this.name,this,a),r&&this.element.addEventListener(this.name,this,e),this._$committedValue=e}handleEvent(e){var o,i;typeof this._$committedValue=="function"?this._$committedValue.call((i=(o=this.options)===null||o===void 0?void 0:o.host)!==null&&i!==void 0?i:this.element,e):this._$committedValue.handleEvent(e)}}class er{constructor(e,o,i){this.element=e,this.type=Do,this._$disconnectableChildren=void 0,this._$parent=o,this.options=i}get _$isConnected(){return this._$parent._$isConnected}_$setValue(e){b==null||b({kind:"commit to element binding",element:this.element,value:e,options:this.options}),De(this,e)}}const lo=B.litHtmlPolyfillSupportDevMode;lo==null||lo(et,rt);((io=B.litHtmlVersions)!==null&&io!==void 0?io:B.litHtmlVersions=[]).push("2.8.0");B.litHtmlVersions.length>1&&Tt("multiple-versions","Multiple versions of Lit loaded. Loading multiple versions is not recommended.");const Ee=(t,e,o)=>{var i,a;if(e==null)throw new TypeError(`The container to render into may not be ${e}`);const n=Rn++,r=(i=o==null?void 0:o.renderBefore)!==null&&i!==void 0?i:e;let l=r._$litPart$;if(b==null||b({kind:"begin render",id:n,value:t,container:e,options:o,part:l}),l===void 0){const s=(a=o==null?void 0:o.renderBefore)!==null&&a!==void 0?a:null;r._$litPart$=l=new rt(e.insertBefore(Qe(),s),s,void 0,o??{})}return l._$setValue(t),b==null||b({kind:"end render",id:n,value:t,container:e,options:o,part:l}),l};Ee.setSanitizer=In,Ee.createSanitizer=Co,Ee._testOnlyClearSanitizerFactoryDoNotCallOrElse=Pn;/**
 * @license
 * Copyright 2017 Google LLC
 * SPDX-License-Identifier: BSD-3-Clause
 */var co,mo,po;let Uo;{const t=(co=globalThis.litIssuedWarnings)!==null&&co!==void 0?co:globalThis.litIssuedWarnings=new Set;Uo=(e,o)=>{o+=` See https://lit.dev/msg/${e} for more information.`,t.has(o)||(console.warn(o),t.add(o))}}class j extends ne{constructor(){super(...arguments),this.renderOptions={host:this},this.__childPart=void 0}createRenderRoot(){var e,o;const i=super.createRenderRoot();return(e=(o=this.renderOptions).renderBefore)!==null&&e!==void 0||(o.renderBefore=i.firstChild),i}update(e){const o=this.render();this.hasUpdated||(this.renderOptions.isConnected=this.isConnected),super.update(e),this.__childPart=Ee(o,this.renderRoot,this.renderOptions)}connectedCallback(){var e;super.connectedCallback(),(e=this.__childPart)===null||e===void 0||e.setConnected(!0)}disconnectedCallback(){var e;super.disconnectedCallback(),(e=this.__childPart)===null||e===void 0||e.setConnected(!1)}render(){return ze}}j.finalized=!0;j._$litElement$=!0;(mo=globalThis.litElementHydrateSupport)===null||mo===void 0||mo.call(globalThis,{LitElement:j});const uo=globalThis.litElementPolyfillSupportDevMode;uo==null||uo({LitElement:j});j.finalize=function(){if(!ne.finalize.call(this))return!1;const e=(o,i,a=!1)=>{if(o.hasOwnProperty(i)){const n=(typeof o=="function"?o:o.constructor).name;Uo(a?"renamed-api":"removed-api",`\`${i}\` is implemented on class ${n}. It has been ${a?"renamed":"removed"} in this version of LitElement.`)}};return e(this,"render"),e(this,"getStyles",!0),e(this.prototype,"adoptStyles"),!0};((po=globalThis.litElementVersions)!==null&&po!==void 0?po:globalThis.litElementVersions=[]).push("3.3.3");globalThis.litElementVersions.length>1&&Uo("multiple-versions","Multiple versions of Lit loaded. Loading multiple versions is not recommended.");/**
 * @license
 * Copyright 2017 Google LLC
 * SPDX-License-Identifier: BSD-3-Clause
 */const tr=(t,e)=>(customElements.define(t,e),e),or=(t,e)=>{const{kind:o,elements:i}=e;return{kind:o,elements:i,finisher(a){customElements.define(t,a)}}},V=t=>e=>typeof e=="function"?tr(t,e):or(t,e);/**
 * @license
 * Copyright 2017 Google LLC
 * SPDX-License-Identifier: BSD-3-Clause
 */const ir=(t,e)=>e.kind==="method"&&e.descriptor&&!("value"in e.descriptor)?{...e,finisher(o){o.createProperty(e.key,t)}}:{kind:"field",key:Symbol(),placement:"own",descriptor:{},originalKey:e.key,initializer(){typeof e.initializer=="function"&&(this[e.key]=e.initializer.call(this))},finisher(o){o.createProperty(e.key,t)}},ar=(t,e,o)=>{e.constructor.createProperty(o,t)};function w(t){return(e,o)=>o!==void 0?ar(t,e,o):ir(t,e)}/**
 * @license
 * Copyright 2017 Google LLC
 * SPDX-License-Identifier: BSD-3-Clause
 */function I(t){return w({...t,state:!0})}/**
 * @license
 * Copyright 2017 Google LLC
 * SPDX-License-Identifier: BSD-3-Clause
 */const nr=({finisher:t,descriptor:e})=>(o,i)=>{var a;if(i!==void 0){const n=o.constructor;e!==void 0&&Object.defineProperty(o,i,e(i)),t==null||t(n,i)}else{const n=(a=o.originalKey)!==null&&a!==void 0?a:o.key,r=e!=null?{kind:"method",placement:"prototype",key:n,descriptor:e(o.key)}:{...o,key:n};return t!=null&&(r.finisher=function(l){t(l,n)}),r}};/**
 * @license
 * Copyright 2017 Google LLC
 * SPDX-License-Identifier: BSD-3-Clause
 */function st(t,e){return nr({descriptor:o=>{const i={get(){var a,n;return(n=(a=this.renderRoot)===null||a===void 0?void 0:a.querySelector(t))!==null&&n!==void 0?n:null},enumerable:!0,configurable:!0};if(e){const a=typeof o=="symbol"?Symbol():`__${o}`;i.get=function(){var n,r;return this[a]===void 0&&(this[a]=(r=(n=this.renderRoot)===null||n===void 0?void 0:n.querySelector(t))!==null&&r!==void 0?r:null),this[a]}}return i}})}/**
 * @license
 * Copyright 2021 Google LLC
 * SPDX-License-Identifier: BSD-3-Clause
 */var ho;const rr=window;((ho=rr.HTMLSlotElement)===null||ho===void 0?void 0:ho.prototype.assignedElements)!=null;/**
 * @license
 * Copyright 2017 Google LLC
 * SPDX-License-Identifier: BSD-3-Clause
 */const sr={ATTRIBUTE:1,CHILD:2,PROPERTY:3,BOOLEAN_ATTRIBUTE:4,EVENT:5,ELEMENT:6},lr=t=>(...e)=>({_$litDirective$:t,values:e});class cr{constructor(e){}get _$isConnected(){return this._$parent._$isConnected}_$initialize(e,o,i){this.__part=e,this._$parent=o,this.__attributeIndex=i}_$resolve(e,o){return this.update(e,o)}update(e,o){return this.render(...o)}}/**
 * @license
 * Copyright 2018 Google LLC
 * SPDX-License-Identifier: BSD-3-Clause
 */class dr extends cr{constructor(e){var o;if(super(e),e.type!==sr.ATTRIBUTE||e.name!=="class"||((o=e.strings)===null||o===void 0?void 0:o.length)>2)throw new Error("`classMap()` can only be used in the `class` attribute and must be the only part in the attribute.")}render(e){return" "+Object.keys(e).filter(o=>e[o]).join(" ")+" "}update(e,[o]){var i,a;if(this._previousClasses===void 0){this._previousClasses=new Set,e.strings!==void 0&&(this._staticClasses=new Set(e.strings.join(" ").split(/\s/).filter(r=>r!=="")));for(const r in o)o[r]&&!(!((i=this._staticClasses)===null||i===void 0)&&i.has(r))&&this._previousClasses.add(r);return this.render(o)}const n=e.element.classList;this._previousClasses.forEach(r=>{r in o||(n.remove(r),this._previousClasses.delete(r))});for(const r in o){const l=!!o[r];l!==this._previousClasses.has(r)&&!(!((a=this._staticClasses)===null||a===void 0)&&a.has(r))&&(l?(n.add(r),this._previousClasses.add(r)):(n.remove(r),this._previousClasses.delete(r)))}return ze}}const qo=lr(dr),go="css-loading-indicator";var J;(function(t){t.IDLE="",t.FIRST="first",t.SECOND="second",t.THIRD="third"})(J||(J={}));class O extends j{constructor(){super(),this.firstDelay=450,this.secondDelay=1500,this.thirdDelay=5e3,this.expandedDuration=2e3,this.onlineText="Online",this.offlineText="Connection lost",this.reconnectingText="Connection lost, trying to reconnect...",this.offline=!1,this.reconnecting=!1,this.expanded=!1,this.loading=!1,this.loadingBarState=J.IDLE,this.applyDefaultThemeState=!0,this.firstTimeout=0,this.secondTimeout=0,this.thirdTimeout=0,this.expandedTimeout=0,this.lastMessageState=R.CONNECTED,this.connectionStateListener=()=>{this.expanded=this.updateConnectionState(),this.expandedTimeout=this.timeoutFor(this.expandedTimeout,this.expanded,()=>{this.expanded=!1},this.expandedDuration)}}static create(){var e,o;const i=window;return!((e=i.Vaadin)===null||e===void 0)&&e.connectionIndicator||(i.Vaadin=i.Vaadin||{},i.Vaadin.connectionIndicator=document.createElement("vaadin-connection-indicator"),document.body.appendChild(i.Vaadin.connectionIndicator)),(o=i.Vaadin)===null||o===void 0?void 0:o.connectionIndicator}render(){return y`
      <div class="v-loading-indicator ${this.loadingBarState}" style=${this.getLoadingBarStyle()}></div>

      <div
        class="v-status-message ${qo({active:this.reconnecting})}"
      >
        <span class="text"> ${this.renderMessage()} </span>
      </div>
    `}connectedCallback(){var e;super.connectedCallback();const o=window;!((e=o.Vaadin)===null||e===void 0)&&e.connectionState&&(this.connectionStateStore=o.Vaadin.connectionState,this.connectionStateStore.addStateChangeListener(this.connectionStateListener),this.updateConnectionState()),this.updateTheme()}disconnectedCallback(){super.disconnectedCallback(),this.connectionStateStore&&this.connectionStateStore.removeStateChangeListener(this.connectionStateListener),this.updateTheme()}get applyDefaultTheme(){return this.applyDefaultThemeState}set applyDefaultTheme(e){e!==this.applyDefaultThemeState&&(this.applyDefaultThemeState=e,this.updateTheme())}createRenderRoot(){return this}updateConnectionState(){var e;const o=(e=this.connectionStateStore)===null||e===void 0?void 0:e.state;return this.offline=o===R.CONNECTION_LOST,this.reconnecting=o===R.RECONNECTING,this.updateLoading(o===R.LOADING),this.loading?!1:o!==this.lastMessageState?(this.lastMessageState=o,!0):!1}updateLoading(e){this.loading=e,this.loadingBarState=J.IDLE,this.firstTimeout=this.timeoutFor(this.firstTimeout,e,()=>{this.loadingBarState=J.FIRST},this.firstDelay),this.secondTimeout=this.timeoutFor(this.secondTimeout,e,()=>{this.loadingBarState=J.SECOND},this.secondDelay),this.thirdTimeout=this.timeoutFor(this.thirdTimeout,e,()=>{this.loadingBarState=J.THIRD},this.thirdDelay)}renderMessage(){return this.reconnecting?this.reconnectingText:this.offline?this.offlineText:this.onlineText}updateTheme(){if(this.applyDefaultThemeState&&this.isConnected){if(!document.getElementById(go)){const e=document.createElement("style");e.id=go,e.textContent=this.getDefaultStyle(),document.head.appendChild(e)}}else{const e=document.getElementById(go);e&&document.head.removeChild(e)}}getDefaultStyle(){return`
      @keyframes v-progress-start {
        0% {
          width: 0%;
        }
        100% {
          width: 50%;
        }
      }
      @keyframes v-progress-delay {
        0% {
          width: 50%;
        }
        100% {
          width: 90%;
        }
      }
      @keyframes v-progress-wait {
        0% {
          width: 90%;
          height: 4px;
        }
        3% {
          width: 91%;
          height: 7px;
        }
        100% {
          width: 96%;
          height: 7px;
        }
      }
      @keyframes v-progress-wait-pulse {
        0% {
          opacity: 1;
        }
        50% {
          opacity: 0.1;
        }
        100% {
          opacity: 1;
        }
      }
      .v-loading-indicator,
      .v-status-message {
        position: fixed;
        z-index: 251;
        left: 0;
        right: auto;
        top: 0;
        background-color: var(--lumo-primary-color, var(--material-primary-color, blue));
        transition: none;
      }
      .v-loading-indicator {
        width: 50%;
        height: 4px;
        opacity: 1;
        pointer-events: none;
        animation: v-progress-start 1000ms 200ms both;
      }
      .v-loading-indicator[style*='none'] {
        display: block !important;
        width: 100%;
        opacity: 0;
        animation: none;
        transition: opacity 500ms 300ms, width 300ms;
      }
      .v-loading-indicator.second {
        width: 90%;
        animation: v-progress-delay 3.8s forwards;
      }
      .v-loading-indicator.third {
        width: 96%;
        animation: v-progress-wait 5s forwards, v-progress-wait-pulse 1s 4s infinite backwards;
      }

      vaadin-connection-indicator[offline] .v-loading-indicator,
      vaadin-connection-indicator[reconnecting] .v-loading-indicator {
        display: none;
      }

      .v-status-message {
        opacity: 0;
        width: 100%;
        max-height: var(--status-height-collapsed, 8px);
        overflow: hidden;
        background-color: var(--status-bg-color-online, var(--lumo-primary-color, var(--material-primary-color, blue)));
        color: var(
          --status-text-color-online,
          var(--lumo-primary-contrast-color, var(--material-primary-contrast-color, #fff))
        );
        font-size: 0.75rem;
        font-weight: 600;
        line-height: 1;
        transition: all 0.5s;
        padding: 0 0.5em;
      }

      vaadin-connection-indicator[offline] .v-status-message,
      vaadin-connection-indicator[reconnecting] .v-status-message {
        opacity: 1;
        background-color: var(--status-bg-color-offline, var(--lumo-shade, #333));
        color: var(
          --status-text-color-offline,
          var(--lumo-primary-contrast-color, var(--material-primary-contrast-color, #fff))
        );
        background-image: repeating-linear-gradient(
          45deg,
          rgba(255, 255, 255, 0),
          rgba(255, 255, 255, 0) 10px,
          rgba(255, 255, 255, 0.1) 10px,
          rgba(255, 255, 255, 0.1) 20px
        );
      }

      vaadin-connection-indicator[reconnecting] .v-status-message {
        animation: show-reconnecting-status 2s;
      }

      vaadin-connection-indicator[offline] .v-status-message:hover,
      vaadin-connection-indicator[reconnecting] .v-status-message:hover,
      vaadin-connection-indicator[expanded] .v-status-message {
        max-height: var(--status-height, 1.75rem);
      }

      vaadin-connection-indicator[expanded] .v-status-message {
        opacity: 1;
      }

      .v-status-message span {
        display: flex;
        align-items: center;
        justify-content: center;
        height: var(--status-height, 1.75rem);
      }

      vaadin-connection-indicator[reconnecting] .v-status-message span::before {
        content: '';
        width: 1em;
        height: 1em;
        border-top: 2px solid
          var(--status-spinner-color, var(--lumo-primary-color, var(--material-primary-color, blue)));
        border-left: 2px solid
          var(--status-spinner-color, var(--lumo-primary-color, var(--material-primary-color, blue)));
        border-right: 2px solid transparent;
        border-bottom: 2px solid transparent;
        border-radius: 50%;
        box-sizing: border-box;
        animation: v-spin 0.4s linear infinite;
        margin: 0 0.5em;
      }

      @keyframes v-spin {
        100% {
          transform: rotate(360deg);
        }
      }
    `}getLoadingBarStyle(){switch(this.loadingBarState){case J.IDLE:return"display: none";case J.FIRST:case J.SECOND:case J.THIRD:return"display: block";default:return""}}timeoutFor(e,o,i,a){return e!==0&&window.clearTimeout(e),o?window.setTimeout(i,a):0}static get instance(){return O.create()}}H([w({type:Number})],O.prototype,"firstDelay",void 0);H([w({type:Number})],O.prototype,"secondDelay",void 0);H([w({type:Number})],O.prototype,"thirdDelay",void 0);H([w({type:Number})],O.prototype,"expandedDuration",void 0);H([w({type:String})],O.prototype,"onlineText",void 0);H([w({type:String})],O.prototype,"offlineText",void 0);H([w({type:String})],O.prototype,"reconnectingText",void 0);H([w({type:Boolean,reflect:!0})],O.prototype,"offline",void 0);H([w({type:Boolean,reflect:!0})],O.prototype,"reconnecting",void 0);H([w({type:Boolean,reflect:!0})],O.prototype,"expanded",void 0);H([w({type:Boolean,reflect:!0})],O.prototype,"loading",void 0);H([w({type:String})],O.prototype,"loadingBarState",void 0);H([w({type:Boolean})],O.prototype,"applyDefaultTheme",null);customElements.get("vaadin-connection-indicator")===void 0&&customElements.define("vaadin-connection-indicator",O);O.instance;const tt=window;tt.Vaadin=tt.Vaadin||{};tt.Vaadin.registrations=tt.Vaadin.registrations||[];tt.Vaadin.registrations.push({is:"@vaadin/common-frontend",version:"0.0.18"});class Ai extends Error{}const Ge=window.document.body,C=window;class mr{constructor(e){this.response=void 0,this.pathname="",this.isActive=!1,this.baseRegex=/^\//,this.navigation="",Ge.$=Ge.$||[],this.config=e||{},C.Vaadin=C.Vaadin||{},C.Vaadin.Flow=C.Vaadin.Flow||{},C.Vaadin.Flow.clients={TypeScript:{isActive:()=>this.isActive}};const o=document.head.querySelector("base");this.baseRegex=new RegExp(`^${(document.baseURI||o&&o.href||"/").replace(/^https?:\/\/[^/]+/i,"")}`),this.appShellTitle=document.title,this.addConnectionIndicator()}get serverSideRoutes(){return[{path:"(.*)",action:this.action}]}loadingStarted(){this.isActive=!0,C.Vaadin.connectionState.loadingStarted()}loadingFinished(){this.isActive=!1,C.Vaadin.connectionState.loadingFinished(),!C.Vaadin.listener&&(C.Vaadin.listener={},document.addEventListener("click",e=>{e.target&&(e.target.hasAttribute("router-link")?this.navigation="link":e.composedPath().some(o=>o.nodeName==="A")&&(this.navigation="client"))},{capture:!0}))}get action(){return async e=>{if(this.pathname=e.pathname,C.Vaadin.connectionState.online)try{await this.flowInit()}catch(o){if(o instanceof Ai)return C.Vaadin.connectionState.state=R.CONNECTION_LOST,this.offlineStubAction();throw o}else return this.offlineStubAction();return this.container.onBeforeEnter=(o,i)=>this.flowNavigate(o,i),this.container.onBeforeLeave=(o,i)=>this.flowLeave(o,i),this.container}}async flowLeave(e,o){const{connectionState:i}=C.Vaadin;return this.pathname===e.pathname||!this.isFlowClientLoaded()||i.offline?Promise.resolve({}):new Promise(a=>{this.loadingStarted(),this.container.serverConnected=n=>{a(o&&n?o.prevent():{}),this.loadingFinished()},Ge.$server.leaveNavigation(this.getFlowRoutePath(e),this.getFlowRouteQuery(e))})}async flowNavigate(e,o){return this.response?new Promise(i=>{this.loadingStarted(),this.container.serverConnected=(a,n)=>{o&&a?i(o.prevent()):o&&o.redirect&&n?i(o.redirect(n.pathname)):(this.container.style.display="",i(this.container)),this.loadingFinished()},this.container.serverPaused=()=>{this.loadingFinished()},Ge.$server.connectClient(this.getFlowRoutePath(e),this.getFlowRouteQuery(e),this.appShellTitle,history.state,this.navigation),this.navigation="history"}):Promise.resolve(this.container)}getFlowRoutePath(e){return decodeURIComponent(e.pathname).replace(this.baseRegex,"")}getFlowRouteQuery(e){return e.search&&e.search.substring(1)||""}async flowInit(){if(!this.isFlowClientLoaded()){this.loadingStarted(),this.response=await this.flowInitUi();const{pushScript:e,appConfig:o}=this.response;typeof e=="string"&&await this.loadScript(e);const{appId:i}=o;await(await v(()=>import("./FlowBootstrap-feff2646.js"),[],import.meta.url)).init(this.response),typeof this.config.imports=="function"&&(this.injectAppIdScript(i),await this.config.imports());const n=`flow-container-${i.toLowerCase()}`,r=document.querySelector(n);r?this.container=r:(this.container=document.createElement(n),this.container.id=i),Ge.$[i]=this.container;const l=await v(()=>import("./FlowClient-341d667e.js"),[],import.meta.url);await this.flowInitClient(l),this.loadingFinished()}return this.container&&!this.container.isConnected&&(this.container.style.display="none",document.body.appendChild(this.container)),this.response}async loadScript(e){return new Promise((o,i)=>{const a=document.createElement("script");a.onload=()=>o(),a.onerror=i,a.src=e,document.body.appendChild(a)})}injectAppIdScript(e){const o=e.substring(0,e.lastIndexOf("-")),i=document.createElement("script");i.type="module",i.setAttribute("data-app-id",o),document.body.append(i)}async flowInitClient(e){return e.init(),new Promise(o=>{const i=setInterval(()=>{Object.keys(C.Vaadin.Flow.clients).filter(n=>n!=="TypeScript").reduce((n,r)=>n||C.Vaadin.Flow.clients[r].isActive(),!1)||(clearInterval(i),o())},5)})}async flowInitUi(){const e=C.Vaadin&&C.Vaadin.TypeScript&&C.Vaadin.TypeScript.initial;return e?(C.Vaadin.TypeScript.initial=void 0,Promise.resolve(e)):new Promise((o,i)=>{const n=new XMLHttpRequest,r=`?v-r=init&location=${encodeURIComponent(this.getFlowRoutePath(location))}&query=${encodeURIComponent(this.getFlowRouteQuery(location))}`;n.open("GET",r),n.onerror=()=>i(new Ai(`Invalid server response when initializing Flow UI.
        ${n.status}
        ${n.responseText}`)),n.onload=()=>{const l=n.getResponseHeader("content-type");l&&l.indexOf("application/json")!==-1?o(JSON.parse(n.responseText)):n.onerror()},n.send()})}addConnectionIndicator(){O.create(),C.addEventListener("online",()=>{if(!this.isFlowClientLoaded()){C.Vaadin.connectionState.state=R.RECONNECTING;const e=new XMLHttpRequest;e.open("HEAD","sw.js"),e.onload=()=>{C.Vaadin.connectionState.state=R.CONNECTED},e.onerror=()=>{C.Vaadin.connectionState.state=R.CONNECTION_LOST},setTimeout(()=>e.send(),50)}}),C.addEventListener("offline",()=>{this.isFlowClientLoaded()||(C.Vaadin.connectionState.state=R.CONNECTION_LOST)})}async offlineStubAction(){const e=document.createElement("iframe"),o="./offline-stub.html";e.setAttribute("src",o),e.setAttribute("style","width: 100%; height: 100%; border: 0"),this.response=void 0;let i;const a=()=>{i!==void 0&&(C.Vaadin.connectionState.removeStateChangeListener(i),i=void 0)};return e.onBeforeEnter=(n,r,l)=>{i=()=>{C.Vaadin.connectionState.online&&(a(),l.render(n,!1))},C.Vaadin.connectionState.addStateChangeListener(i)},e.onBeforeLeave=(n,r,l)=>{a()},e}isFlowClientLoaded(){return this.response!==void 0}}const{serverSideRoutes:pr}=new mr({imports:()=>v(()=>import("./generated-flow-imports-6737655e.js"),[],import.meta.url)}),ur=[...pr],hr=new ge(document.querySelector("#outlet"));hr.setRoutes(ur);(function(){if(typeof document>"u"||"adoptedStyleSheets"in document)return;var t="ShadyCSS"in window&&!ShadyCSS.nativeShadow,e=document.implementation.createHTMLDocument(""),o=new WeakMap,i=typeof DOMException=="object"?Error:DOMException,a=Object.defineProperty,n=Array.prototype.forEach,r=/@import.+?;?$/gm;function l(p){var u=p.replace(r,"");return u!==p&&console.warn("@import rules are not allowed here. See https://github.com/WICG/construct-stylesheets/issues/119#issuecomment-588352418"),u.trim()}function s(p){return"isConnected"in p?p.isConnected:document.contains(p)}function c(p){return p.filter(function(u,_){return p.indexOf(u)===_})}function d(p,u){return p.filter(function(_){return u.indexOf(_)===-1})}function h(p){p.parentNode.removeChild(p)}function m(p){return p.shadowRoot||o.get(p)}var x=["addRule","deleteRule","insertRule","removeRule"],pe=CSSStyleSheet,ue=pe.prototype;ue.replace=function(){return Promise.reject(new i("Can't call replace on non-constructed CSSStyleSheets."))},ue.replaceSync=function(){throw new i("Failed to execute 'replaceSync' on 'CSSStyleSheet': Can't call replaceSync on non-constructed CSSStyleSheets.")};function re(p){return typeof p=="object"?Ae.isPrototypeOf(p)||ue.isPrototypeOf(p):!1}function qt(p){return typeof p=="object"?ue.isPrototypeOf(p):!1}var W=new WeakMap,oe=new WeakMap,$e=new WeakMap,je=new WeakMap;function Ft(p,u){var _=document.createElement("style");return $e.get(p).set(u,_),oe.get(p).push(u),_}function se(p,u){return $e.get(p).get(u)}function lt(p,u){$e.get(p).delete(u),oe.set(p,oe.get(p).filter(function(_){return _!==u}))}function Qo(p,u){requestAnimationFrame(function(){u.textContent=W.get(p).textContent,je.get(p).forEach(function(_){return u.sheet[_.method].apply(u.sheet,_.args)})})}function ct(p){if(!W.has(p))throw new TypeError("Illegal invocation")}function Bt(){var p=this,u=document.createElement("style");e.body.appendChild(u),W.set(p,u),oe.set(p,[]),$e.set(p,new WeakMap),je.set(p,[])}var Ae=Bt.prototype;Ae.replace=function(u){try{return this.replaceSync(u),Promise.resolve(this)}catch(_){return Promise.reject(_)}},Ae.replaceSync=function(u){if(ct(this),typeof u=="string"){var _=this;W.get(_).textContent=l(u),je.set(_,[]),oe.get(_).forEach(function(D){D.isConnected()&&Qo(_,se(_,D))})}},a(Ae,"cssRules",{configurable:!0,enumerable:!0,get:function(){return ct(this),W.get(this).sheet.cssRules}}),a(Ae,"media",{configurable:!0,enumerable:!0,get:function(){return ct(this),W.get(this).sheet.media}}),x.forEach(function(p){Ae[p]=function(){var u=this;ct(u);var _=arguments;je.get(u).push({method:p,args:_}),oe.get(u).forEach(function(q){if(q.isConnected()){var L=se(u,q).sheet;L[p].apply(L,_)}});var D=W.get(u).sheet;return D[p].apply(D,_)}}),a(Bt,Symbol.hasInstance,{configurable:!0,value:re});var Zo={childList:!0,subtree:!0},ei=new WeakMap;function Re(p){var u=ei.get(p);return u||(u=new ii(p),ei.set(p,u)),u}function ti(p){a(p.prototype,"adoptedStyleSheets",{configurable:!0,enumerable:!0,get:function(){return Re(this).sheets},set:function(u){Re(this).update(u)}})}function Ht(p,u){for(var _=document.createNodeIterator(p,NodeFilter.SHOW_ELEMENT,function(q){return m(q)?NodeFilter.FILTER_ACCEPT:NodeFilter.FILTER_REJECT},null,!1),D=void 0;D=_.nextNode();)u(m(D))}var dt=new WeakMap,Ne=new WeakMap,mt=new WeakMap;function Oa(p,u){return u instanceof HTMLStyleElement&&Ne.get(p).some(function(_){return se(_,p)})}function oi(p){var u=dt.get(p);return u instanceof Document?u.body:u}function Wt(p){var u=document.createDocumentFragment(),_=Ne.get(p),D=mt.get(p),q=oi(p);D.disconnect(),_.forEach(function(L){u.appendChild(se(L,p)||Ft(L,p))}),q.insertBefore(u,null),D.observe(q,Zo),_.forEach(function(L){Qo(L,se(L,p))})}function ii(p){var u=this;u.sheets=[],dt.set(u,p),Ne.set(u,[]),mt.set(u,new MutationObserver(function(_,D){if(!document){D.disconnect();return}_.forEach(function(q){t||n.call(q.addedNodes,function(L){L instanceof Element&&Ht(L,function(Ie){Re(Ie).connect()})}),n.call(q.removedNodes,function(L){L instanceof Element&&(Oa(u,L)&&Wt(u),t||Ht(L,function(Ie){Re(Ie).disconnect()}))})})}))}if(ii.prototype={isConnected:function(){var p=dt.get(this);return p instanceof Document?p.readyState!=="loading":s(p.host)},connect:function(){var p=oi(this);mt.get(this).observe(p,Zo),Ne.get(this).length>0&&Wt(this),Ht(p,function(u){Re(u).connect()})},disconnect:function(){mt.get(this).disconnect()},update:function(p){var u=this,_=dt.get(u)===document?"Document":"ShadowRoot";if(!Array.isArray(p))throw new TypeError("Failed to set the 'adoptedStyleSheets' property on "+_+": Iterator getter is not callable.");if(!p.every(re))throw new TypeError("Failed to set the 'adoptedStyleSheets' property on "+_+": Failed to convert value to 'CSSStyleSheet'");if(p.some(qt))throw new TypeError("Failed to set the 'adoptedStyleSheets' property on "+_+": Can't adopt non-constructed stylesheets");u.sheets=p;var D=Ne.get(u),q=c(p),L=d(D,q);L.forEach(function(Ie){h(se(Ie,u)),lt(Ie,u)}),Ne.set(u,q),u.isConnected()&&q.length>0&&Wt(u)}},window.CSSStyleSheet=Bt,ti(Document),"ShadowRoot"in window){ti(ShadowRoot);var ai=Element.prototype,La=ai.attachShadow;ai.attachShadow=function(u){var _=La.call(this,u);return u.mode==="closed"&&o.set(this,_),_}}var pt=Re(document);pt.isConnected()?pt.connect():document.addEventListener("DOMContentLoaded",pt.connect.bind(pt))})();/**
 * @license
 * Copyright 2020 Google LLC
 * SPDX-License-Identifier: BSD-3-Clause
 */const ka=Symbol.for(""),gr=t=>{if((t==null?void 0:t.r)===ka)return t==null?void 0:t._$litStatic$},fr=t=>{if(t._$litStatic$!==void 0)return t._$litStatic$;throw new Error(`Value passed to 'literal' function must be a 'literal' result: ${t}. Use 'unsafeStatic' to pass non-literal values, but
            take care to ensure page security.`)},gt=(t,...e)=>({_$litStatic$:e.reduce((o,i,a)=>o+fr(i)+t[a+1],t[0]),r:ka}),Ri=new Map,vr=t=>(e,...o)=>{const i=o.length;let a,n;const r=[],l=[];let s=0,c=!1,d;for(;s<i;){for(d=e[s];s<i&&(n=o[s],(a=gr(n))!==void 0);)d+=a+e[++s],c=!0;s!==i&&l.push(n),r.push(d),s++}if(s===i&&r.push(e[i]),c){const h=r.join("$$lit$$");e=Ri.get(h),e===void 0&&(r.raw=r,Ri.set(h,e=r)),o=l}return t(e,...o)},xr=vr(y),yr="modulepreload",br=function(t){return"/"+t},Ni={},f=function(t,e,o){if(!e||e.length===0)return t();const i=document.getElementsByTagName("link");return Promise.all(e.map(a=>{if(a=br(a),a in Ni)return;Ni[a]=!0;const n=a.endsWith(".css"),r=n?'[rel="stylesheet"]':"";if(o)for(let s=i.length-1;s>=0;s--){const c=i[s];if(c.href===a&&(!n||c.rel==="stylesheet"))return}else if(document.querySelector(`link[href="${a}"]${r}`))return;const l=document.createElement("link");if(l.rel=n?"stylesheet":yr,n||(l.as="script",l.crossOrigin=""),l.href=a,document.head.appendChild(l),n)return new Promise((s,c)=>{l.addEventListener("load",s),l.addEventListener("error",()=>c(new Error(`Unable to preload CSS for ${a}`)))})})).then(()=>t()).catch(a=>{const n=new Event("vite:preloadError",{cancelable:!0});if(n.payload=a,window.dispatchEvent(n),!n.defaultPrevented)throw a})};function g(t,e,o,i){var a=arguments.length,n=a<3?e:i===null?i=Object.getOwnPropertyDescriptor(e,o):i,r;if(typeof Reflect=="object"&&typeof Reflect.decorate=="function")n=Reflect.decorate(t,e,o,i);else for(var l=t.length-1;l>=0;l--)(r=t[l])&&(n=(a<3?r(n):a>3?r(e,o,n):r(e,o))||n);return a>3&&n&&Object.defineProperty(e,o,n),n}function wr(t){var e;const o=[];for(;t&&t.parentNode;){const i=To(t);if(i.nodeId!==-1){if((e=i.element)!=null&&e.tagName.startsWith("FLOW-CONTAINER-"))break;o.push(i)}t=t.parentElement?t.parentElement:t.parentNode.host}return o.reverse()}function To(t){const e=window.Vaadin;if(e&&e.Flow){const{clients:o}=e.Flow,i=Object.keys(o);for(const a of i){const n=o[a];if(n.getNodeId){const r=n.getNodeId(t);if(r>=0)return{nodeId:r,uiId:n.getUIId(),element:t}}}}return{nodeId:-1,uiId:-1,element:void 0}}function _r(t,e){if(t.contains(e))return!0;let o=e;const i=e.ownerDocument;for(;o&&o!==i&&o!==t;)o=o.parentNode||(o instanceof ShadowRoot?o.host:null);return o===t}const kr=(t,e)=>{const o=t[e];return o?typeof o=="function"?o():Promise.resolve(o):new Promise((i,a)=>{(typeof queueMicrotask=="function"?queueMicrotask:setTimeout)(a.bind(null,new Error("Unknown variable dynamic import: "+e)))})};var N;(function(t){t.text="text",t.checkbox="checkbox",t.range="range",t.color="color"})(N||(N={}));const te={lumoSize:["--lumo-size-xs","--lumo-size-s","--lumo-size-m","--lumo-size-l","--lumo-size-xl"],lumoSpace:["--lumo-space-xs","--lumo-space-s","--lumo-space-m","--lumo-space-l","--lumo-space-xl"],lumoBorderRadius:["0","--lumo-border-radius-m","--lumo-border-radius-l"],lumoFontSize:["--lumo-font-size-xxs","--lumo-font-size-xs","--lumo-font-size-s","--lumo-font-size-m","--lumo-font-size-l","--lumo-font-size-xl","--lumo-font-size-xxl","--lumo-font-size-xxxl"],lumoTextColor:["--lumo-header-text-color","--lumo-body-text-color","--lumo-secondary-text-color","--lumo-tertiary-text-color","--lumo-disabled-text-color","--lumo-primary-text-color","--lumo-error-text-color","--lumo-success-text-color"],basicBorderSize:["0px","1px","2px","3px"]},Er=Object.freeze(Object.defineProperty({__proto__:null,presets:te},Symbol.toStringTag,{value:"Module"})),ce={textColor:{propertyName:"color",displayName:"Text color",editorType:N.color,presets:te.lumoTextColor},fontSize:{propertyName:"font-size",displayName:"Font size",editorType:N.range,presets:te.lumoFontSize,icon:"font"},fontWeight:{propertyName:"font-weight",displayName:"Bold",editorType:N.checkbox,checkedValue:"bold"},fontStyle:{propertyName:"font-style",displayName:"Italic",editorType:N.checkbox,checkedValue:"italic"}},Z={backgroundColor:{propertyName:"background-color",displayName:"Background color",editorType:N.color},borderColor:{propertyName:"border-color",displayName:"Border color",editorType:N.color},borderWidth:{propertyName:"border-width",displayName:"Border width",editorType:N.range,presets:te.basicBorderSize,icon:"square"},borderRadius:{propertyName:"border-radius",displayName:"Border radius",editorType:N.range,presets:te.lumoBorderRadius,icon:"square"},padding:{propertyName:"padding",displayName:"Padding",editorType:N.range,presets:te.lumoSpace,icon:"square"},gap:{propertyName:"gap",displayName:"Spacing",editorType:N.range,presets:te.lumoSpace,icon:"square"}},Sr={height:{propertyName:"height",displayName:"Size",editorType:N.range,presets:te.lumoSize,icon:"square"},paddingInline:{propertyName:"padding-inline",displayName:"Padding",editorType:N.range,presets:te.lumoSpace,icon:"square"}},$o={iconColor:{propertyName:"color",displayName:"Icon color",editorType:N.color,presets:te.lumoTextColor},iconSize:{propertyName:"font-size",displayName:"Icon size",editorType:N.range,presets:te.lumoFontSize,icon:"font"}},Cr=[Z.backgroundColor,Z.borderColor,Z.borderWidth,Z.borderRadius,Z.padding],zr=[ce.textColor,ce.fontSize,ce.fontWeight,ce.fontStyle],Tr=[$o.iconColor,$o.iconSize],$r=Object.freeze(Object.defineProperty({__proto__:null,fieldProperties:Sr,iconProperties:$o,shapeProperties:Z,standardIconProperties:Tr,standardShapeProperties:Cr,standardTextProperties:zr,textProperties:ce},Symbol.toStringTag,{value:"Module"}));function Ea(t){const e=t.charAt(0).toUpperCase()+t.slice(1);return{tagName:t,displayName:e,elements:[{selector:t,displayName:"Element",properties:[Z.backgroundColor,Z.borderColor,Z.borderWidth,Z.borderRadius,Z.padding,ce.textColor,ce.fontSize,ce.fontWeight,ce.fontStyle]}]}}const jr=Object.freeze(Object.defineProperty({__proto__:null,createGenericMetadata:Ea},Symbol.toStringTag,{value:"Module"})),Ar=t=>kr(Object.assign({"./components/defaults.ts":()=>f(()=>Promise.resolve().then(()=>$r),void 0),"./components/generic.ts":()=>f(()=>Promise.resolve().then(()=>jr),void 0),"./components/presets.ts":()=>f(()=>Promise.resolve().then(()=>Er),void 0),"./components/vaadin-accordion-heading.ts":()=>f(()=>v(()=>import("./vaadin-accordion-heading-c0acdd6d-e18cfab6.js"),[],import.meta.url),[]),"./components/vaadin-accordion-panel.ts":()=>f(()=>v(()=>import("./vaadin-accordion-panel-616e55d6-c34542cc.js"),[],import.meta.url),[]),"./components/vaadin-accordion.ts":()=>f(()=>v(()=>import("./vaadin-accordion-eed3b794-8fa25225.js"),[],import.meta.url),[]),"./components/vaadin-app-layout.ts":()=>f(()=>v(()=>import("./vaadin-app-layout-e56de2e9-0e679f7f.js"),[],import.meta.url),[]),"./components/vaadin-avatar.ts":()=>f(()=>v(()=>import("./vaadin-avatar-7599297d-20694880.js"),[],import.meta.url),[]),"./components/vaadin-big-decimal-field.ts":()=>f(()=>v(()=>import("./vaadin-big-decimal-field-e51def24-0025ab51.js"),["./vaadin-big-decimal-field-e51def24-0025ab51.js","./vaadin-text-field-0b3db014-bd47b1b4.js","./vaadin-button-2511ad84-663611ac.js"],import.meta.url),["assets/vaadin-big-decimal-field-e51def24.js","assets/vaadin-text-field-0b3db014.js","assets/vaadin-button-2511ad84.js"]),"./components/vaadin-board-row.ts":()=>f(()=>v(()=>import("./vaadin-board-row-c70d0c55-03417b2a.js"),[],import.meta.url),[]),"./components/vaadin-board.ts":()=>f(()=>v(()=>import("./vaadin-board-828ebdea-f7362bc2.js"),[],import.meta.url),[]),"./components/vaadin-button.ts":()=>f(()=>v(()=>import("./vaadin-button-2511ad84-663611ac.js"),[],import.meta.url),[]),"./components/vaadin-chart.ts":()=>f(()=>v(()=>import("./vaadin-chart-5192dc15-294d41ca.js"),[],import.meta.url),[]),"./components/vaadin-checkbox-group.ts":()=>f(()=>v(()=>import("./vaadin-checkbox-group-a7c65bf2-4745b544.js"),["./vaadin-checkbox-group-a7c65bf2-4745b544.js","./vaadin-text-field-0b3db014-bd47b1b4.js","./vaadin-checkbox-4e68df64-c7dc2916.js"],import.meta.url),["assets/vaadin-checkbox-group-a7c65bf2.js","assets/vaadin-text-field-0b3db014.js","assets/vaadin-checkbox-4e68df64.js"]),"./components/vaadin-checkbox.ts":()=>f(()=>v(()=>import("./vaadin-checkbox-4e68df64-c7dc2916.js"),[],import.meta.url),[]),"./components/vaadin-combo-box.ts":()=>f(()=>v(()=>import("./vaadin-combo-box-96451ddd-d2af5b30.js"),["./vaadin-combo-box-96451ddd-d2af5b30.js","./vaadin-text-field-0b3db014-bd47b1b4.js"],import.meta.url),["assets/vaadin-combo-box-96451ddd.js","assets/vaadin-text-field-0b3db014.js"]),"./components/vaadin-confirm-dialog.ts":()=>f(()=>v(()=>import("./vaadin-confirm-dialog-4d718829-b35329b1.js"),["./vaadin-confirm-dialog-4d718829-b35329b1.js","./vaadin-button-2511ad84-663611ac.js"],import.meta.url),["assets/vaadin-confirm-dialog-4d718829.js","assets/vaadin-button-2511ad84.js"]),"./components/vaadin-cookie-consent.ts":()=>f(()=>v(()=>import("./vaadin-cookie-consent-46c09f8b-d8cd0593.js"),[],import.meta.url),[]),"./components/vaadin-crud.ts":()=>f(()=>v(()=>import("./vaadin-crud-8d161a22-0b9c4607.js"),[],import.meta.url),[]),"./components/vaadin-custom-field.ts":()=>f(()=>v(()=>import("./vaadin-custom-field-42c85b9e-07586551.js"),["./vaadin-custom-field-42c85b9e-07586551.js","./vaadin-text-field-0b3db014-bd47b1b4.js"],import.meta.url),["assets/vaadin-custom-field-42c85b9e.js","assets/vaadin-text-field-0b3db014.js"]),"./components/vaadin-date-picker.ts":()=>f(()=>v(()=>import("./vaadin-date-picker-f2001167-36ed678a.js"),["./vaadin-date-picker-f2001167-36ed678a.js","./vaadin-text-field-0b3db014-bd47b1b4.js"],import.meta.url),["assets/vaadin-date-picker-f2001167.js","assets/vaadin-text-field-0b3db014.js"]),"./components/vaadin-date-time-picker.ts":()=>f(()=>v(()=>import("./vaadin-date-time-picker-c8c047a7-537e93b8.js"),["./vaadin-date-time-picker-c8c047a7-537e93b8.js","./vaadin-text-field-0b3db014-bd47b1b4.js"],import.meta.url),["assets/vaadin-date-time-picker-c8c047a7.js","assets/vaadin-text-field-0b3db014.js"]),"./components/vaadin-details-summary.ts":()=>f(()=>v(()=>import("./vaadin-details-summary-351a1448-bd82b360.js"),[],import.meta.url),[]),"./components/vaadin-details.ts":()=>f(()=>v(()=>import("./vaadin-details-bf336660-1195ebe0.js"),[],import.meta.url),[]),"./components/vaadin-dialog.ts":()=>f(()=>v(()=>import("./vaadin-dialog-53253a08-30cf860b.js"),[],import.meta.url),[]),"./components/vaadin-email-field.ts":()=>f(()=>v(()=>import("./vaadin-email-field-d7a35f04-43970f68.js"),["./vaadin-email-field-d7a35f04-43970f68.js","./vaadin-text-field-0b3db014-bd47b1b4.js","./vaadin-button-2511ad84-663611ac.js"],import.meta.url),["assets/vaadin-email-field-d7a35f04.js","assets/vaadin-text-field-0b3db014.js","assets/vaadin-button-2511ad84.js"]),"./components/vaadin-form-layout.ts":()=>f(()=>v(()=>import("./vaadin-form-layout-47744b1d-a54e56c5.js"),[],import.meta.url),[]),"./components/vaadin-grid-pro.ts":()=>f(()=>v(()=>import("./vaadin-grid-pro-ff415555-76cd4afd.js"),["./vaadin-grid-pro-ff415555-76cd4afd.js","./vaadin-checkbox-4e68df64-c7dc2916.js","./vaadin-grid-0a4791c2-4b9e9a6d.js","./vaadin-text-field-0b3db014-bd47b1b4.js"],import.meta.url),["assets/vaadin-grid-pro-ff415555.js","assets/vaadin-checkbox-4e68df64.js","assets/vaadin-grid-0a4791c2.js","assets/vaadin-text-field-0b3db014.js"]),"./components/vaadin-grid.ts":()=>f(()=>v(()=>import("./vaadin-grid-0a4791c2-4b9e9a6d.js"),["./vaadin-grid-0a4791c2-4b9e9a6d.js","./vaadin-checkbox-4e68df64-c7dc2916.js"],import.meta.url),["assets/vaadin-grid-0a4791c2.js","assets/vaadin-checkbox-4e68df64.js"]),"./components/vaadin-horizontal-layout.ts":()=>f(()=>v(()=>import("./vaadin-horizontal-layout-3193943f-5b4da1d2.js"),[],import.meta.url),[]),"./components/vaadin-icon.ts":()=>f(()=>v(()=>import("./vaadin-icon-601f36ed-8eac0dc9.js"),[],import.meta.url),[]),"./components/vaadin-integer-field.ts":()=>f(()=>v(()=>import("./vaadin-integer-field-85078932-b9eede23.js"),["./vaadin-integer-field-85078932-b9eede23.js","./vaadin-text-field-0b3db014-bd47b1b4.js","./vaadin-button-2511ad84-663611ac.js"],import.meta.url),["assets/vaadin-integer-field-85078932.js","assets/vaadin-text-field-0b3db014.js","assets/vaadin-button-2511ad84.js"]),"./components/vaadin-list-box.ts":()=>f(()=>v(()=>import("./vaadin-list-box-d7a8433b-024cdf07.js"),[],import.meta.url),[]),"./components/vaadin-login-form.ts":()=>f(()=>v(()=>import("./vaadin-login-form-638996c6-a738050d.js"),["./vaadin-login-form-638996c6-a738050d.js","./vaadin-text-field-0b3db014-bd47b1b4.js","./vaadin-button-2511ad84-663611ac.js"],import.meta.url),["assets/vaadin-login-form-638996c6.js","assets/vaadin-text-field-0b3db014.js","assets/vaadin-button-2511ad84.js"]),"./components/vaadin-login-overlay.ts":()=>f(()=>v(()=>import("./vaadin-login-overlay-f8a5db8a-cff9b04d.js"),["./vaadin-login-overlay-f8a5db8a-cff9b04d.js","./vaadin-text-field-0b3db014-bd47b1b4.js","./vaadin-button-2511ad84-663611ac.js"],import.meta.url),["assets/vaadin-login-overlay-f8a5db8a.js","assets/vaadin-text-field-0b3db014.js","assets/vaadin-button-2511ad84.js"]),"./components/vaadin-map.ts":()=>f(()=>v(()=>import("./vaadin-map-d40a0116-f11a2e3c.js"),[],import.meta.url),[]),"./components/vaadin-menu-bar.ts":()=>f(()=>v(()=>import("./vaadin-menu-bar-3f5ab096-f7e68232.js"),[],import.meta.url),[]),"./components/vaadin-message-input.ts":()=>f(()=>v(()=>import("./vaadin-message-input-996ac37c-4ca0e352.js"),["./vaadin-message-input-996ac37c-4ca0e352.js","./vaadin-text-field-0b3db014-bd47b1b4.js"],import.meta.url),["assets/vaadin-message-input-996ac37c.js","assets/vaadin-text-field-0b3db014.js"]),"./components/vaadin-message-list.ts":()=>f(()=>v(()=>import("./vaadin-message-list-70a435ba-bdba4b82.js"),[],import.meta.url),[]),"./components/vaadin-multi-select-combo-box.ts":()=>f(()=>v(()=>import("./vaadin-multi-select-combo-box-a3373557-eca93aaa.js"),["./vaadin-multi-select-combo-box-a3373557-eca93aaa.js","./vaadin-text-field-0b3db014-bd47b1b4.js"],import.meta.url),["assets/vaadin-multi-select-combo-box-a3373557.js","assets/vaadin-text-field-0b3db014.js"]),"./components/vaadin-notification.ts":()=>f(()=>v(()=>import("./vaadin-notification-bd6eb776-78a403c4.js"),[],import.meta.url),[]),"./components/vaadin-number-field.ts":()=>f(()=>v(()=>import("./vaadin-number-field-cb3ee8b2-0bef0fc6.js"),["./vaadin-number-field-cb3ee8b2-0bef0fc6.js","./vaadin-text-field-0b3db014-bd47b1b4.js","./vaadin-button-2511ad84-663611ac.js"],import.meta.url),["assets/vaadin-number-field-cb3ee8b2.js","assets/vaadin-text-field-0b3db014.js","assets/vaadin-button-2511ad84.js"]),"./components/vaadin-password-field.ts":()=>f(()=>v(()=>import("./vaadin-password-field-d289cb18-07640cf1.js"),["./vaadin-password-field-d289cb18-07640cf1.js","./vaadin-text-field-0b3db014-bd47b1b4.js","./vaadin-button-2511ad84-663611ac.js"],import.meta.url),["assets/vaadin-password-field-d289cb18.js","assets/vaadin-text-field-0b3db014.js","assets/vaadin-button-2511ad84.js"]),"./components/vaadin-progress-bar.ts":()=>f(()=>v(()=>import("./vaadin-progress-bar-309ecf1f-1ff063fd.js"),[],import.meta.url),[]),"./components/vaadin-radio-group.ts":()=>f(()=>v(()=>import("./vaadin-radio-group-88b5afd8-09043dea.js"),["./vaadin-radio-group-88b5afd8-09043dea.js","./vaadin-text-field-0b3db014-bd47b1b4.js"],import.meta.url),["assets/vaadin-radio-group-88b5afd8.js","assets/vaadin-text-field-0b3db014.js"]),"./components/vaadin-rich-text-editor.ts":()=>f(()=>v(()=>import("./vaadin-rich-text-editor-8cd892f2-fd7ccee0.js"),[],import.meta.url),[]),"./components/vaadin-scroller.ts":()=>f(()=>v(()=>import("./vaadin-scroller-35e68818-789c680b.js"),[],import.meta.url),[]),"./components/vaadin-select.ts":()=>f(()=>v(()=>import("./vaadin-select-df6e9947-bf5997d9.js"),["./vaadin-select-df6e9947-bf5997d9.js","./vaadin-text-field-0b3db014-bd47b1b4.js"],import.meta.url),["assets/vaadin-select-df6e9947.js","assets/vaadin-text-field-0b3db014.js"]),"./components/vaadin-side-nav-item.ts":()=>f(()=>v(()=>import("./vaadin-side-nav-item-34918f92-05b7d50e.js"),[],import.meta.url),[]),"./components/vaadin-side-nav.ts":()=>f(()=>v(()=>import("./vaadin-side-nav-ba80d91d-130f2bd1.js"),[],import.meta.url),[]),"./components/vaadin-split-layout.ts":()=>f(()=>v(()=>import("./vaadin-split-layout-80c92131-a1721770.js"),[],import.meta.url),[]),"./components/vaadin-spreadsheet.ts":()=>f(()=>v(()=>import("./vaadin-spreadsheet-59d8c5ef-d6ce2dd6.js"),[],import.meta.url),[]),"./components/vaadin-tab.ts":()=>f(()=>v(()=>import("./vaadin-tab-aaf32809-c16f4c73.js"),[],import.meta.url),[]),"./components/vaadin-tabs.ts":()=>f(()=>v(()=>import("./vaadin-tabs-d9a5e24e-e18c5ed8.js"),[],import.meta.url),[]),"./components/vaadin-tabsheet.ts":()=>f(()=>v(()=>import("./vaadin-tabsheet-dd99ed9a-acf327c6.js"),[],import.meta.url),[]),"./components/vaadin-text-area.ts":()=>f(()=>v(()=>import("./vaadin-text-area-83627ebc-ed14627e.js"),["./vaadin-text-area-83627ebc-ed14627e.js","./vaadin-text-field-0b3db014-bd47b1b4.js","./vaadin-button-2511ad84-663611ac.js"],import.meta.url),["assets/vaadin-text-area-83627ebc.js","assets/vaadin-text-field-0b3db014.js","assets/vaadin-button-2511ad84.js"]),"./components/vaadin-text-field.ts":()=>f(()=>v(()=>import("./vaadin-text-field-0b3db014-bd47b1b4.js"),[],import.meta.url),[]),"./components/vaadin-time-picker.ts":()=>f(()=>v(()=>import("./vaadin-time-picker-715ec415-45c27624.js"),["./vaadin-time-picker-715ec415-45c27624.js","./vaadin-text-field-0b3db014-bd47b1b4.js"],import.meta.url),["assets/vaadin-time-picker-715ec415.js","assets/vaadin-text-field-0b3db014.js"]),"./components/vaadin-upload.ts":()=>f(()=>v(()=>import("./vaadin-upload-d3c162ed-5153c7a6.js"),["./vaadin-upload-d3c162ed-5153c7a6.js","./vaadin-button-2511ad84-663611ac.js"],import.meta.url),["assets/vaadin-upload-d3c162ed.js","assets/vaadin-button-2511ad84.js"]),"./components/vaadin-vertical-layout.ts":()=>f(()=>v(()=>import("./vaadin-vertical-layout-ad4174c4-8caf851f.js"),[],import.meta.url),[]),"./components/vaadin-virtual-list.ts":()=>f(()=>v(()=>import("./vaadin-virtual-list-96896203-1364e00e.js"),[],import.meta.url),[])}),`./components/${t}.ts`);class Rr{constructor(e=Ar){this.loader=e,this.metadata={}}async getMetadata(e){var o;const i=(o=e.element)==null?void 0:o.localName;if(!i)return null;if(!i.startsWith("vaadin-"))return Ea(i);let a=this.metadata[i];if(a)return a;try{a=(await this.loader(i)).default,this.metadata[i]=a}catch{console.warn(`Failed to load metadata for component: ${i}`)}return a||null}}const Nr=new Rr,kt={crosshair:Pe`<svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor" fill="none" stroke-linecap="round" stroke-linejoin="round">
   <path stroke="none" d="M0 0h24v24H0z" fill="none"></path>
   <path d="M4 8v-2a2 2 0 0 1 2 -2h2"></path>
   <path d="M4 16v2a2 2 0 0 0 2 2h2"></path>
   <path d="M16 4h2a2 2 0 0 1 2 2v2"></path>
   <path d="M16 20h2a2 2 0 0 0 2 -2v-2"></path>
   <path d="M9 12l6 0"></path>
   <path d="M12 9l0 6"></path>
</svg>`,square:Pe`<svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor" fill="currentColor" stroke-linecap="round" stroke-linejoin="round">
   <path stroke="none" d="M0 0h24v24H0z" fill="none"></path>
   <path d="M3 3m0 2a2 2 0 0 1 2 -2h14a2 2 0 0 1 2 2v14a2 2 0 0 1 -2 2h-14a2 2 0 0 1 -2 -2z"></path>
</svg>`,font:Pe`<svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor" fill="none" stroke-linecap="round" stroke-linejoin="round">
   <path stroke="none" d="M0 0h24v24H0z" fill="none"></path>
   <path d="M4 20l3 0"></path>
   <path d="M14 20l7 0"></path>
   <path d="M6.9 15l6.9 0"></path>
   <path d="M10.2 6.3l5.8 13.7"></path>
   <path d="M5 20l6 -16l2 0l7 16"></path>
</svg>`,undo:Pe`<svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor" fill="none" stroke-linecap="round" stroke-linejoin="round">
   <path stroke="none" d="M0 0h24v24H0z" fill="none"></path>
   <path d="M9 13l-4 -4l4 -4m-4 4h11a4 4 0 0 1 0 8h-1"></path>
</svg>`,redo:Pe`<svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor" fill="none" stroke-linecap="round" stroke-linejoin="round">
   <path stroke="none" d="M0 0h24v24H0z" fill="none"></path>
   <path d="M15 13l4 -4l-4 -4m4 4h-11a4 4 0 0 0 0 8h1"></path>
</svg>`,cross:Pe`<svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" stroke-width="3" stroke="currentColor" fill="none" stroke-linecap="round" stroke-linejoin="round">
   <path stroke="none" d="M0 0h24v24H0z" fill="none"></path>
   <path d="M18 6l-12 12"></path>
   <path d="M6 6l12 12"></path>
</svg>`};var Ue;(function(t){t.disabled="disabled",t.enabled="enabled",t.missing_theme="missing_theme"})(Ue||(Ue={}));var P;(function(t){t.local="local",t.global="global"})(P||(P={}));function fo(t,e){return`${t}|${e}`}class ve{constructor(e){this._properties={},this._metadata=e}get metadata(){return this._metadata}get properties(){return Object.values(this._properties)}getPropertyValue(e,o){return this._properties[fo(e,o)]||null}updatePropertyValue(e,o,i,a){if(!i){delete this._properties[fo(e,o)];return}let n=this.getPropertyValue(e,o);n?(n.value=i,n.modified=a||!1):(n={elementSelector:e,propertyName:o,value:i,modified:a||!1},this._properties[fo(e,o)]=n)}addPropertyValues(e){e.forEach(o=>{this.updatePropertyValue(o.elementSelector,o.propertyName,o.value,o.modified)})}getPropertyValuesForElement(e){return this.properties.filter(o=>o.elementSelector===e)}static combine(...e){if(e.length<2)throw new Error("Must provide at least two themes");const o=new ve(e[0].metadata);return e.forEach(i=>o.addPropertyValues(i.properties)),o}static fromServerRules(e,o,i){const a=new ve(e);return e.elements.forEach(n=>{const r=qe(n,o),l=i.find(s=>s.selector===r.replace(/ > /g,">"));l&&n.properties.forEach(s=>{const c=l.properties[s.propertyName];c&&a.updatePropertyValue(n.selector,s.propertyName,c,!0)})}),a}}function qe(t,e){const o=t.selector;if(e.themeScope===P.global)return o;if(!e.localClassName)throw new Error("Can not build local scoped selector without instance class name");const i=o.match(/^[\w\d-_]+/),a=i&&i[0];if(!a)throw new Error(`Selector does not start with a tag name: ${o}`);return`${a}.${e.localClassName}${o.substring(a.length,o.length)}`}function Ir(t,e,o,i){const a=qe(t,e),n={[o]:i};return o==="border-width"&&(parseInt(i)>0?n["border-style"]="solid":n["border-style"]=""),{selector:a,properties:n}}function Pr(t){const e=Object.entries(t.properties).map(([o,i])=>`${o}: ${i};`).join(" ");return`${t.selector} { ${e} }`}let ft,Ii="";function Fo(t){ft||(ft=new CSSStyleSheet,document.adoptedStyleSheets=[...document.adoptedStyleSheets,ft]),Ii+=t.cssText,ft.replaceSync(Ii)}const Sa=k`
  .editor-row {
    display: flex;
    align-items: baseline;
    padding: var(--theme-editor-section-horizontal-padding);
    gap: 10px;
  }

  .editor-row > .label {
    flex: 0 0 auto;
    width: 120px;
  }

  .editor-row > .editor {
    flex: 1 1 0;
  }
`,Pi="__vaadin-theme-editor-measure-element",Oi=/((::before)|(::after))$/,Li=/::part\(([\w\d_-]+)\)$/;Fo(k`
  .__vaadin-theme-editor-measure-element {
    position: absolute;
    top: 0;
    left: 0;
    visibility: hidden;
  }
`);async function Or(t){const e=new ve(t),o=document.createElement(t.tagName);o.classList.add(Pi),document.body.append(o),t.setupElement&&await t.setupElement(o);const i={themeScope:P.local,localClassName:Pi};try{t.elements.forEach(a=>{Mi(o,a,i,!0);let n=qe(a,i);const r=n.match(Oi);n=n.replace(Oi,"");const l=n.match(Li),s=n.replace(Li,"");let c=document.querySelector(s);if(c&&l){const m=`[part~="${l[1]}"]`;c=c.shadowRoot.querySelector(m)}if(!c)return;c.style.transition="none";const d=r?r[1]:null,h=getComputedStyle(c,d);a.properties.forEach(m=>{const x=h.getPropertyValue(m.propertyName)||m.defaultValue||"";e.updatePropertyValue(a.selector,m.propertyName,x)}),Mi(o,a,i,!1)})}finally{try{t.cleanupElement&&await t.cleanupElement(o)}finally{o.remove()}}return e}function Mi(t,e,o,i){if(e.stateAttribute){if(e.stateElementSelector){const a=qe({...e,selector:e.stateElementSelector},o);t=document.querySelector(a)}t&&(i?t.setAttribute(e.stateAttribute,""):t.removeAttribute(e.stateAttribute))}}function Vi(t){return t.trim()}function Lr(t){const e=t.element;if(!e)return null;const o=e.querySelector("label");if(o&&o.textContent)return Vi(o.textContent);const i=e.textContent;return i?Vi(i):null}class Mr{constructor(){this._localClassNameMap=new Map}get stylesheet(){return this.ensureStylesheet(),this._stylesheet}add(e){this.ensureStylesheet(),this._stylesheet.replaceSync(e)}clear(){this.ensureStylesheet(),this._stylesheet.replaceSync("")}previewLocalClassName(e,o){if(!e)return;const i=this._localClassNameMap.get(e);i&&(e.classList.remove(i),e.overlayClass=null),o?(e.classList.add(o),e.overlayClass=o,this._localClassNameMap.set(e,o)):this._localClassNameMap.delete(e)}ensureStylesheet(){this._stylesheet||(this._stylesheet=new CSSStyleSheet,this._stylesheet.replaceSync(""),document.adoptedStyleSheets=[...document.adoptedStyleSheets,this._stylesheet])}}const we=new Mr;var X;(function(t){t.response="themeEditorResponse",t.loadComponentMetadata="themeEditorComponentMetadata",t.setLocalClassName="themeEditorLocalClassName",t.setCssRules="themeEditorRules",t.loadRules="themeEditorLoadRules",t.history="themeEditorHistory",t.openCss="themeEditorOpenCss",t.markAsUsed="themeEditorMarkAsUsed"})(X||(X={}));var jo;(function(t){t.ok="ok",t.error="error"})(jo||(jo={}));class Vr{constructor(e){this.pendingRequests={},this.requestCounter=0,this.wrappedConnection=e;const o=this.wrappedConnection.onMessage;this.wrappedConnection.onMessage=i=>{i.command===X.response?this.handleResponse(i.data):o.call(this.wrappedConnection,i)}}sendRequest(e,o){const i=(this.requestCounter++).toString(),a=o.uiId??this.getGlobalUiId();return new Promise((n,r)=>{this.wrappedConnection.send(e,{...o,requestId:i,uiId:a}),this.pendingRequests[i]={resolve:n,reject:r}})}handleResponse(e){const o=this.pendingRequests[e.requestId];if(!o){console.warn("Received response for unknown request");return}delete this.pendingRequests[e.requestId],e.code===jo.ok?o.resolve(e):o.reject(e)}loadComponentMetadata(e){return this.sendRequest(X.loadComponentMetadata,{nodeId:e.nodeId})}setLocalClassName(e,o){return this.sendRequest(X.setLocalClassName,{nodeId:e.nodeId,className:o})}setCssRules(e){return this.sendRequest(X.setCssRules,{rules:e})}loadRules(e){return this.sendRequest(X.loadRules,{selectors:e})}markAsUsed(){return this.sendRequest(X.markAsUsed,{})}undo(e){return this.sendRequest(X.history,{undo:e})}redo(e){return this.sendRequest(X.history,{redo:e})}openCss(e){return this.sendRequest(X.openCss,{selector:e})}getGlobalUiId(){if(this.globalUiId===void 0){const e=window.Vaadin;if(e&&e.Flow){const{clients:o}=e.Flow,i=Object.keys(o);for(const a of i){const n=o[a];if(n.getNodeId){this.globalUiId=n.getUIId();break}}}}return this.globalUiId??-1}}const M={index:-1,entries:[]};class Dr{constructor(e){this.api=e}get allowUndo(){return M.index>=0}get allowRedo(){return M.index<M.entries.length-1}get allowedActions(){return{allowUndo:this.allowUndo,allowRedo:this.allowRedo}}push(e,o,i){const a={requestId:e,execute:o,rollback:i};if(M.index++,M.entries=M.entries.slice(0,M.index),M.entries.push(a),o)try{o()}catch(n){console.error("Execute history entry failed",n)}return this.allowedActions}async undo(){if(!this.allowUndo)return this.allowedActions;const e=M.entries[M.index];M.index--;try{await this.api.undo(e.requestId),e.rollback&&e.rollback()}catch(o){console.error("Undo failed",o)}return this.allowedActions}async redo(){if(!this.allowRedo)return this.allowedActions;M.index++;const e=M.entries[M.index];try{await this.api.redo(e.requestId),e.execute&&e.execute()}catch(o){console.error("Redo failed",o)}return this.allowedActions}static clear(){M.entries=[],M.index=-1}}class Ur extends CustomEvent{constructor(e,o,i){super("theme-property-value-change",{bubbles:!0,composed:!0,detail:{element:e,property:o,value:i}})}}class K extends j{constructor(){super(...arguments),this.value=""}static get styles(){return[Sa,k`
        :host {
          display: block;
        }

        .editor-row .label .modified {
          display: inline-block;
          width: 6px;
          height: 6px;
          background: orange;
          border-radius: 3px;
          margin-left: 3px;
        }
      `]}update(e){super.update(e),(e.has("propertyMetadata")||e.has("theme"))&&this.updateValueFromTheme()}render(){var e;return y`
      <div class="editor-row">
        <div class="label">
          ${this.propertyMetadata.displayName}
          ${(e=this.propertyValue)!=null&&e.modified?y`<span class="modified"></span>`:null}
        </div>
        <div class="editor">${this.renderEditor()}</div>
      </div>
    `}updateValueFromTheme(){var e;this.propertyValue=this.theme.getPropertyValue(this.elementMetadata.selector,this.propertyMetadata.propertyName),this.value=((e=this.propertyValue)==null?void 0:e.value)||""}dispatchChange(e){this.dispatchEvent(new Ur(this.elementMetadata,this.propertyMetadata,e))}}g([w({})],K.prototype,"elementMetadata",void 0);g([w({})],K.prototype,"propertyMetadata",void 0);g([w({})],K.prototype,"theme",void 0);g([I()],K.prototype,"propertyValue",void 0);g([I()],K.prototype,"value",void 0);class At{get values(){return this._values}get rawValues(){return this._rawValues}constructor(e){if(this._values=[],this._rawValues={},e){const o=e.propertyName,i=e.presets??[];this._values=(i||[]).map(n=>n.startsWith("--")?`var(${n})`:n);const a=document.createElement("div");a.style.borderStyle="solid",a.style.visibility="hidden",document.body.append(a);try{this._values.forEach(n=>{a.style.setProperty(o,n);const r=getComputedStyle(a);this._rawValues[n]=r.getPropertyValue(o).trim()})}finally{a.remove()}}}tryMapToRawValue(e){return this._rawValues[e]??e}tryMapToPreset(e){return this.findPreset(e)??e}findPreset(e){const o=e&&e.trim();return this.values.find(i=>this._rawValues[i]===o)}}class Di extends CustomEvent{constructor(e){super("change",{detail:{value:e}})}}let Rt=class extends j{constructor(){super(...arguments),this.value="",this.showClearButton=!1}static get styles(){return k`
      :host {
        display: inline-block;
        width: 100%;
        position: relative;
      }

      input {
        width: 100%;
        box-sizing: border-box;
        padding: 0.25rem 0.375rem;
        color: inherit;
        background: rgba(0, 0, 0, 0.2);
        border-radius: 0.25rem;
        border: none;
      }

      button {
        display: none;
        position: absolute;
        right: 4px;
        top: 4px;
        padding: 0;
        line-height: 0;
        border: none;
        background: none;
        color: var(--dev-tools-text-color);
      }

      button svg {
        width: 16px;
        height: 16px;
      }

      button:not(:disabled):hover {
        color: var(--dev-tools-text-color-emphasis);
      }

      :host(.show-clear-button) input {
        padding-right: 20px;
      }

      :host(.show-clear-button) button {
        display: block;
      }
    `}update(t){super.update(t),t.has("showClearButton")&&(this.showClearButton?this.classList.add("show-clear-button"):this.classList.remove("show-clear-button"))}render(){return y`
      <input class="input" .value=${this.value} @change=${this.handleInputChange} />
      <button @click=${this.handleClearClick}>${kt.cross}</button>
    `}handleInputChange(t){const e=t.target;this.dispatchEvent(new Di(e.value))}handleClearClick(){this.dispatchEvent(new Di(""))}};g([w({})],Rt.prototype,"value",void 0);g([w({})],Rt.prototype,"showClearButton",void 0);Rt=g([V("vaadin-dev-tools-theme-text-input")],Rt);class qr extends CustomEvent{constructor(e){super("class-name-change",{detail:{value:e}})}}let ot=class extends j{constructor(){super(...arguments),this.editedClassName="",this.invalid=!1}static get styles(){return[Sa,k`
        .editor-row {
          padding-top: 0;
        }

        .editor-row .editor .error {
          display: inline-block;
          color: var(--dev-tools-red-color);
          margin-top: 4px;
        }
      `]}update(t){super.update(t),t.has("className")&&(this.editedClassName=this.className,this.invalid=!1)}render(){return y` <div class="editor-row local-class-name">
      <div class="label">CSS class name</div>
      <div class="editor">
        <vaadin-dev-tools-theme-text-input
          type="text"
          .value=${this.editedClassName}
          @change=${this.handleInputChange}
        ></vaadin-dev-tools-theme-text-input>
        ${this.invalid?y`<br /><span class="error">Please enter a valid CSS class name</span>`:null}
      </div>
    </div>`}handleInputChange(t){this.editedClassName=t.detail.value;const e=/^-?[_a-zA-Z]+[_a-zA-Z0-9-]*$/;this.invalid=!this.editedClassName.match(e),!this.invalid&&this.editedClassName!==this.className&&this.dispatchEvent(new qr(this.editedClassName))}};g([w({})],ot.prototype,"className",void 0);g([I()],ot.prototype,"editedClassName",void 0);g([I()],ot.prototype,"invalid",void 0);ot=g([V("vaadin-dev-tools-theme-class-name-editor")],ot);class Fr extends CustomEvent{constructor(e){super("scope-change",{detail:{value:e}})}}Fo(k`
  vaadin-select-overlay[theme~='vaadin-dev-tools-theme-scope-selector'] {
    --lumo-primary-color-50pct: rgba(255, 255, 255, 0.5);
    z-index: 100000 !important;
  }

  vaadin-select-overlay[theme~='vaadin-dev-tools-theme-scope-selector']::part(overlay) {
    background: #333;
  }

  vaadin-select-overlay[theme~='vaadin-dev-tools-theme-scope-selector'] vaadin-item {
    color: rgba(255, 255, 255, 0.8);
  }

  vaadin-select-overlay[theme~='vaadin-dev-tools-theme-scope-selector'] vaadin-item::part(content) {
    font-size: 13px;
  }

  vaadin-select-overlay[theme~='vaadin-dev-tools-theme-scope-selector'] vaadin-item .title {
    color: rgba(255, 255, 255, 0.95);
    font-weight: bold;
  }

  vaadin-select-overlay[theme~='vaadin-dev-tools-theme-scope-selector'] vaadin-item::part(checkmark) {
    margin: 6px;
  }

  vaadin-select-overlay[theme~='vaadin-dev-tools-theme-scope-selector'] vaadin-item::part(checkmark)::before {
    color: rgba(255, 255, 255, 0.95);
  }

  vaadin-select-overlay[theme~='vaadin-dev-tools-theme-scope-selector'] vaadin-item:hover {
    background: rgba(255, 255, 255, 0.1);
  }
`);let it=class extends j{constructor(){super(...arguments),this.value=P.local}static get styles(){return k`
      vaadin-select {
        --lumo-primary-color-50pct: rgba(255, 255, 255, 0.5);
        width: 100px;
      }

      vaadin-select::part(input-field) {
        background: rgba(0, 0, 0, 0.2);
      }

      vaadin-select vaadin-select-value-button,
      vaadin-select::part(toggle-button) {
        color: var(--dev-tools-text-color);
      }

      vaadin-select:hover vaadin-select-value-button,
      vaadin-select:hover::part(toggle-button) {
        color: var(--dev-tools-text-color-emphasis);
      }

      vaadin-select vaadin-select-item {
        font-size: 13px;
      }
    `}update(t){var e;super.update(t),t.has("metadata")&&((e=this.select)==null||e.requestContentUpdate())}render(){return y` <vaadin-select
      theme="small vaadin-dev-tools-theme-scope-selector"
      .value=${this.value}
      .renderer=${this.selectRenderer.bind(this)}
      @value-changed=${this.handleValueChange}
    ></vaadin-select>`}selectRenderer(t){var e;const o=((e=this.metadata)==null?void 0:e.displayName)||"Component",i=`${o}s`;Ee(y`
        <vaadin-list-box>
          <vaadin-item value=${P.local} label="Local">
            <span class="title">Local</span>
            <br />
            <span>Edit styles for this ${o}</span>
          </vaadin-item>
          <vaadin-item value=${P.global} label="Global">
            <span class="title">Global</span>
            <br />
            <span>Edit styles for all ${i}</span>
          </vaadin-item>
        </vaadin-list-box>
      `,t)}handleValueChange(t){const e=t.detail.value;e!==this.value&&this.dispatchEvent(new Fr(e))}};g([w({})],it.prototype,"value",void 0);g([w({})],it.prototype,"metadata",void 0);g([st("vaadin-select")],it.prototype,"select",void 0);it=g([V("vaadin-dev-tools-theme-scope-selector")],it);let Ui=class extends K{static get styles(){return[K.styles,k`
        .editor-row {
          align-items: center;
        }
      `]}handleInputChange(t){const e=t.target.checked?this.propertyMetadata.checkedValue:"";this.dispatchChange(e||"")}renderEditor(){const t=this.value===this.propertyMetadata.checkedValue;return y` <input type="checkbox" .checked=${t} @change=${this.handleInputChange} /> `}};Ui=g([V("vaadin-dev-tools-theme-checkbox-property-editor")],Ui);let qi=class extends K{handleInputChange(t){this.dispatchChange(t.detail.value)}renderEditor(){var t;return y`
      <vaadin-dev-tools-theme-text-input
        .value=${this.value}
        .showClearButton=${((t=this.propertyValue)==null?void 0:t.modified)||!1}
        @change=${this.handleInputChange}
      ></vaadin-dev-tools-theme-text-input>
    `}};qi=g([V("vaadin-dev-tools-theme-text-property-editor")],qi);let Nt=class extends K{constructor(){super(...arguments),this.selectedPresetIndex=-1,this.presets=new At}static get styles(){return[K.styles,k`
        :host {
          --preset-count: 3;
          --slider-bg: #fff;
          --slider-border: #333;
        }

        .editor-row {
          align-items: center;
        }

        .editor-row > .editor {
          display: flex;
          align-items: center;
          gap: 1rem;
        }

        .editor-row .input {
          flex: 0 0 auto;
          width: 80px;
        }

        .slider-wrapper {
          flex: 1 1 0;
          display: flex;
          align-items: center;
          gap: 0.5rem;
        }

        .icon {
          width: 20px;
          height: 20px;
          color: #aaa;
        }

        .icon.prefix > svg {
          transform: scale(0.75);
        }

        .slider {
          flex: 1 1 0;
          -webkit-appearance: none;
          background: linear-gradient(to right, #666, #666 2px, transparent 2px);
          background-size: calc((100% - 13px) / (var(--preset-count) - 1)) 8px;
          background-position: 5px 50%;
          background-repeat: repeat-x;
        }

        .slider::-webkit-slider-runnable-track {
          width: 100%;
          box-sizing: border-box;
          height: 16px;
          background-image: linear-gradient(#666, #666);
          background-size: calc(100% - 12px) 2px;
          background-repeat: no-repeat;
          background-position: 6px 50%;
        }

        .slider::-moz-range-track {
          width: 100%;
          box-sizing: border-box;
          height: 16px;
          background-image: linear-gradient(#666, #666);
          background-size: calc(100% - 12px) 2px;
          background-repeat: no-repeat;
          background-position: 6px 50%;
        }

        .slider::-webkit-slider-thumb {
          -webkit-appearance: none;
          height: 16px;
          width: 16px;
          border: 2px solid var(--slider-border);
          border-radius: 50%;
          background: var(--slider-bg);
          cursor: pointer;
        }

        .slider::-moz-range-thumb {
          height: 16px;
          width: 16px;
          border: 2px solid var(--slider-border);
          border-radius: 50%;
          background: var(--slider-bg);
          cursor: pointer;
        }

        .custom-value {
          opacity: 0.5;
        }

        .custom-value:hover,
        .custom-value:focus-within {
          opacity: 1;
        }

        .custom-value:not(:hover, :focus-within) {
          --slider-bg: #333;
          --slider-border: #666;
        }
      `]}update(t){t.has("propertyMetadata")&&(this.presets=new At(this.propertyMetadata)),super.update(t)}renderEditor(){var t;const e={"slider-wrapper":!0,"custom-value":this.selectedPresetIndex<0},o=this.presets.values.length;return y`
      <div class=${qo(e)}>
        ${null}
        <input
          type="range"
          class="slider"
          style="--preset-count: ${o}"
          step="1"
          min="0"
          .max=${(o-1).toString()}
          .value=${this.selectedPresetIndex}
          @input=${this.handleSliderInput}
          @change=${this.handleSliderChange}
        />
        ${null}
      </div>
      <vaadin-dev-tools-theme-text-input
        class="input"
        .value=${this.value}
        .showClearButton=${((t=this.propertyValue)==null?void 0:t.modified)||!1}
        @change=${this.handleValueChange}
      ></vaadin-dev-tools-theme-text-input>
    `}handleSliderInput(t){const e=t.target,o=parseInt(e.value),i=this.presets.values[o];this.selectedPresetIndex=o,this.value=this.presets.rawValues[i]}handleSliderChange(){this.dispatchChange(this.value)}handleValueChange(t){this.value=t.detail.value,this.updateSliderValue(),this.dispatchChange(this.value)}dispatchChange(t){const e=this.presets.tryMapToPreset(t);super.dispatchChange(e)}updateValueFromTheme(){var t;super.updateValueFromTheme(),this.value=this.presets.tryMapToRawValue(((t=this.propertyValue)==null?void 0:t.value)||""),this.updateSliderValue()}updateSliderValue(){const t=this.presets.findPreset(this.value);this.selectedPresetIndex=t?this.presets.values.indexOf(t):-1}};g([I()],Nt.prototype,"selectedPresetIndex",void 0);g([I()],Nt.prototype,"presets",void 0);Nt=g([V("vaadin-dev-tools-theme-range-property-editor")],Nt);const Fe=(t,e=0,o=1)=>t>o?o:t<e?e:t,U=(t,e=0,o=Math.pow(10,e))=>Math.round(o*t)/o,Ca=({h:t,s:e,v:o,a:i})=>{const a=(200-e)*o/100;return{h:U(t),s:U(a>0&&a<200?e*o/100/(a<=100?a:200-a)*100:0),l:U(a/2),a:U(i,2)}},Ao=t=>{const{h:e,s:o,l:i}=Ca(t);return`hsl(${e}, ${o}%, ${i}%)`},vo=t=>{const{h:e,s:o,l:i,a}=Ca(t);return`hsla(${e}, ${o}%, ${i}%, ${a})`},Br=({h:t,s:e,v:o,a:i})=>{t=t/360*6,e=e/100,o=o/100;const a=Math.floor(t),n=o*(1-e),r=o*(1-(t-a)*e),l=o*(1-(1-t+a)*e),s=a%6;return{r:U([o,r,n,n,l,o][s]*255),g:U([l,o,o,r,n,n][s]*255),b:U([n,n,l,o,o,r][s]*255),a:U(i,2)}},Hr=t=>{const{r:e,g:o,b:i,a}=Br(t);return`rgba(${e}, ${o}, ${i}, ${a})`},Wr=t=>{const e=/rgba?\(?\s*(-?\d*\.?\d+)(%)?[,\s]+(-?\d*\.?\d+)(%)?[,\s]+(-?\d*\.?\d+)(%)?,?\s*[/\s]*(-?\d*\.?\d+)?(%)?\s*\)?/i.exec(t);return e?Gr({r:Number(e[1])/(e[2]?100/255:1),g:Number(e[3])/(e[4]?100/255:1),b:Number(e[5])/(e[6]?100/255:1),a:e[7]===void 0?1:Number(e[7])/(e[8]?100:1)}):{h:0,s:0,v:0,a:1}},Gr=({r:t,g:e,b:o,a:i})=>{const a=Math.max(t,e,o),n=a-Math.min(t,e,o),r=n?a===t?(e-o)/n:a===e?2+(o-t)/n:4+(t-e)/n:0;return{h:U(60*(r<0?r+6:r)),s:U(a?n/a*100:0),v:U(a/255*100),a:i}},Kr=(t,e)=>{if(t===e)return!0;for(const o in t)if(t[o]!==e[o])return!1;return!0},Yr=(t,e)=>t.replace(/\s/g,"")===e.replace(/\s/g,""),Fi={},za=t=>{let e=Fi[t];return e||(e=document.createElement("template"),e.innerHTML=t,Fi[t]=e),e},Bo=(t,e,o)=>{t.dispatchEvent(new CustomEvent(e,{bubbles:!0,detail:o}))};let Le=!1;const Ro=t=>"touches"in t,Jr=t=>Le&&!Ro(t)?!1:(Le||(Le=Ro(t)),!0),Bi=(t,e)=>{const o=Ro(e)?e.touches[0]:e,i=t.el.getBoundingClientRect();Bo(t.el,"move",t.getMove({x:Fe((o.pageX-(i.left+window.pageXOffset))/i.width),y:Fe((o.pageY-(i.top+window.pageYOffset))/i.height)}))},Xr=(t,e)=>{const o=e.keyCode;o>40||t.xy&&o<37||o<33||(e.preventDefault(),Bo(t.el,"move",t.getMove({x:o===39?.01:o===37?-.01:o===34?.05:o===33?-.05:o===35?1:o===36?-1:0,y:o===40?.01:o===38?-.01:0},!0)))};class Ho{constructor(e,o,i,a){const n=za(`<div role="slider" tabindex="0" part="${o}" ${i}><div part="${o}-pointer"></div></div>`);e.appendChild(n.content.cloneNode(!0));const r=e.querySelector(`[part=${o}]`);r.addEventListener("mousedown",this),r.addEventListener("touchstart",this),r.addEventListener("keydown",this),this.el=r,this.xy=a,this.nodes=[r.firstChild,r]}set dragging(e){const o=e?document.addEventListener:document.removeEventListener;o(Le?"touchmove":"mousemove",this),o(Le?"touchend":"mouseup",this)}handleEvent(e){switch(e.type){case"mousedown":case"touchstart":if(e.preventDefault(),!Jr(e)||!Le&&e.button!=0)return;this.el.focus(),Bi(this,e),this.dragging=!0;break;case"mousemove":case"touchmove":e.preventDefault(),Bi(this,e);break;case"mouseup":case"touchend":this.dragging=!1;break;case"keydown":Xr(this,e);break}}style(e){e.forEach((o,i)=>{for(const a in o)this.nodes[i].style.setProperty(a,o[a])})}}class Qr extends Ho{constructor(e){super(e,"hue",'aria-label="Hue" aria-valuemin="0" aria-valuemax="360"',!1)}update({h:e}){this.h=e,this.style([{left:`${e/360*100}%`,color:Ao({h:e,s:100,v:100,a:1})}]),this.el.setAttribute("aria-valuenow",`${U(e)}`)}getMove(e,o){return{h:o?Fe(this.h+e.x*360,0,360):360*e.x}}}class Zr extends Ho{constructor(e){super(e,"saturation",'aria-label="Color"',!0)}update(e){this.hsva=e,this.style([{top:`${100-e.v}%`,left:`${e.s}%`,color:Ao(e)},{"background-color":Ao({h:e.h,s:100,v:100,a:1})}]),this.el.setAttribute("aria-valuetext",`Saturation ${U(e.s)}%, Brightness ${U(e.v)}%`)}getMove(e,o){return{s:o?Fe(this.hsva.s+e.x*100,0,100):e.x*100,v:o?Fe(this.hsva.v-e.y*100,0,100):Math.round(100-e.y*100)}}}const es=':host{display:flex;flex-direction:column;position:relative;width:200px;height:200px;user-select:none;-webkit-user-select:none;cursor:default}:host([hidden]){display:none!important}[role=slider]{position:relative;touch-action:none;user-select:none;-webkit-user-select:none;outline:0}[role=slider]:last-child{border-radius:0 0 8px 8px}[part$=pointer]{position:absolute;z-index:1;box-sizing:border-box;width:28px;height:28px;display:flex;place-content:center center;transform:translate(-50%,-50%);background-color:#fff;border:2px solid #fff;border-radius:50%;box-shadow:0 2px 4px rgba(0,0,0,.2)}[part$=pointer]::after{content:"";width:100%;height:100%;border-radius:inherit;background-color:currentColor}[role=slider]:focus [part$=pointer]{transform:translate(-50%,-50%) scale(1.1)}',ts="[part=hue]{flex:0 0 24px;background:linear-gradient(to right,red 0,#ff0 17%,#0f0 33%,#0ff 50%,#00f 67%,#f0f 83%,red 100%)}[part=hue-pointer]{top:50%;z-index:2}",os="[part=saturation]{flex-grow:1;border-color:transparent;border-bottom:12px solid #000;border-radius:8px 8px 0 0;background-image:linear-gradient(to top,#000,transparent),linear-gradient(to right,#fff,rgba(255,255,255,0));box-shadow:inset 0 0 0 1px rgba(0,0,0,.05)}[part=saturation-pointer]{z-index:3}",vt=Symbol("same"),xo=Symbol("color"),Hi=Symbol("hsva"),yo=Symbol("update"),Wi=Symbol("parts"),It=Symbol("css"),Pt=Symbol("sliders");let is=class extends HTMLElement{static get observedAttributes(){return["color"]}get[It](){return[es,ts,os]}get[Pt](){return[Zr,Qr]}get color(){return this[xo]}set color(t){if(!this[vt](t)){const e=this.colorModel.toHsva(t);this[yo](e),this[xo]=t}}constructor(){super();const t=za(`<style>${this[It].join("")}</style>`),e=this.attachShadow({mode:"open"});e.appendChild(t.content.cloneNode(!0)),e.addEventListener("move",this),this[Wi]=this[Pt].map(o=>new o(e))}connectedCallback(){if(this.hasOwnProperty("color")){const t=this.color;delete this.color,this.color=t}else this.color||(this.color=this.colorModel.defaultColor)}attributeChangedCallback(t,e,o){const i=this.colorModel.fromAttr(o);this[vt](i)||(this.color=i)}handleEvent(t){const e=this[Hi],o={...e,...t.detail};this[yo](o);let i;!Kr(o,e)&&!this[vt](i=this.colorModel.fromHsva(o))&&(this[xo]=i,Bo(this,"color-changed",{value:i}))}[vt](t){return this.color&&this.colorModel.equal(t,this.color)}[yo](t){this[Hi]=t,this[Wi].forEach(e=>e.update(t))}};class as extends Ho{constructor(e){super(e,"alpha",'aria-label="Alpha" aria-valuemin="0" aria-valuemax="1"',!1)}update(e){this.hsva=e;const o=vo({...e,a:0}),i=vo({...e,a:1}),a=e.a*100;this.style([{left:`${a}%`,color:vo(e)},{"--gradient":`linear-gradient(90deg, ${o}, ${i}`}]);const n=U(a);this.el.setAttribute("aria-valuenow",`${n}`),this.el.setAttribute("aria-valuetext",`${n}%`)}getMove(e,o){return{a:o?Fe(this.hsva.a+e.x):e.x}}}const ns=`[part=alpha]{flex:0 0 24px}[part=alpha]::after{display:block;content:"";position:absolute;top:0;left:0;right:0;bottom:0;border-radius:inherit;background-image:var(--gradient);box-shadow:inset 0 0 0 1px rgba(0,0,0,.05)}[part^=alpha]{background-color:#fff;background-image:url('data:image/svg+xml,<svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" fill-opacity=".05"><rect x="8" width="8" height="8"/><rect y="8" width="8" height="8"/></svg>')}[part=alpha-pointer]{top:50%}`;class rs extends is{get[It](){return[...super[It],ns]}get[Pt](){return[...super[Pt],as]}}const ss={defaultColor:"rgba(0, 0, 0, 1)",toHsva:Wr,fromHsva:Hr,equal:Yr,fromAttr:t=>t};class ls extends rs{get colorModel(){return ss}}/**
* @license
* Copyright (c) 2017 - 2023 Vaadin Ltd.
* This program is available under Apache License Version 2.0, available at https://vaadin.com/license/
*/function cs(t){const e=[];for(;t;){if(t.nodeType===Node.DOCUMENT_NODE){e.push(t);break}if(t.nodeType===Node.DOCUMENT_FRAGMENT_NODE){e.push(t),t=t.host;continue}if(t.assignedSlot){t=t.assignedSlot;continue}t=t.parentNode}return e}const bo={start:"top",end:"bottom"},wo={start:"left",end:"right"},Gi=new ResizeObserver(t=>{setTimeout(()=>{t.forEach(e=>{e.target.__overlay&&e.target.__overlay._updatePosition()})})}),ds=t=>class extends t{static get properties(){return{positionTarget:{type:Object,value:null},horizontalAlign:{type:String,value:"start"},verticalAlign:{type:String,value:"top"},noHorizontalOverlap:{type:Boolean,value:!1},noVerticalOverlap:{type:Boolean,value:!1},requiredVerticalSpace:{type:Number,value:0}}}static get observers(){return["__positionSettingsChanged(horizontalAlign, verticalAlign, noHorizontalOverlap, noVerticalOverlap, requiredVerticalSpace)","__overlayOpenedChanged(opened, positionTarget)"]}constructor(){super(),this.__onScroll=this.__onScroll.bind(this),this._updatePosition=this._updatePosition.bind(this)}connectedCallback(){super.connectedCallback(),this.opened&&this.__addUpdatePositionEventListeners()}disconnectedCallback(){super.disconnectedCallback(),this.__removeUpdatePositionEventListeners()}__addUpdatePositionEventListeners(){window.addEventListener("resize",this._updatePosition),this.__positionTargetAncestorRootNodes=cs(this.positionTarget),this.__positionTargetAncestorRootNodes.forEach(e=>{e.addEventListener("scroll",this.__onScroll,!0)})}__removeUpdatePositionEventListeners(){window.removeEventListener("resize",this._updatePosition),this.__positionTargetAncestorRootNodes&&(this.__positionTargetAncestorRootNodes.forEach(e=>{e.removeEventListener("scroll",this.__onScroll,!0)}),this.__positionTargetAncestorRootNodes=null)}__overlayOpenedChanged(e,o){if(this.__removeUpdatePositionEventListeners(),o&&(o.__overlay=null,Gi.unobserve(o),e&&(this.__addUpdatePositionEventListeners(),o.__overlay=this,Gi.observe(o))),e){const i=getComputedStyle(this);this.__margins||(this.__margins={},["top","bottom","left","right"].forEach(a=>{this.__margins[a]=parseInt(i[a],10)})),this.setAttribute("dir",i.direction),this._updatePosition(),requestAnimationFrame(()=>this._updatePosition())}}__positionSettingsChanged(){this._updatePosition()}__onScroll(e){this.contains(e.target)||this._updatePosition()}_updatePosition(){if(!this.positionTarget||!this.opened)return;const e=this.positionTarget.getBoundingClientRect(),o=this.__shouldAlignStartVertically(e);this.style.justifyContent=o?"flex-start":"flex-end";const i=this.__isRTL,a=this.__shouldAlignStartHorizontally(e,i),n=!i&&a||i&&!a;this.style.alignItems=n?"flex-start":"flex-end";const r=this.getBoundingClientRect(),l=this.__calculatePositionInOneDimension(e,r,this.noVerticalOverlap,bo,this,o),s=this.__calculatePositionInOneDimension(e,r,this.noHorizontalOverlap,wo,this,a);Object.assign(this.style,l,s),this.toggleAttribute("bottom-aligned",!o),this.toggleAttribute("top-aligned",o),this.toggleAttribute("end-aligned",!n),this.toggleAttribute("start-aligned",n)}__shouldAlignStartHorizontally(e,o){const i=Math.max(this.__oldContentWidth||0,this.$.overlay.offsetWidth);this.__oldContentWidth=this.$.overlay.offsetWidth;const a=Math.min(window.innerWidth,document.documentElement.clientWidth),n=!o&&this.horizontalAlign==="start"||o&&this.horizontalAlign==="end";return this.__shouldAlignStart(e,i,a,this.__margins,n,this.noHorizontalOverlap,wo)}__shouldAlignStartVertically(e){const o=this.requiredVerticalSpace||Math.max(this.__oldContentHeight||0,this.$.overlay.offsetHeight);this.__oldContentHeight=this.$.overlay.offsetHeight;const i=Math.min(window.innerHeight,document.documentElement.clientHeight),a=this.verticalAlign==="top";return this.__shouldAlignStart(e,o,i,this.__margins,a,this.noVerticalOverlap,bo)}__shouldAlignStart(e,o,i,a,n,r,l){const s=i-e[r?l.end:l.start]-a[l.end],c=e[r?l.start:l.end]-a[l.start],d=n?s:c,h=d>(n?c:s)||d>o;return n===h}__adjustBottomProperty(e,o,i){let a;if(e===o.end){if(o.end===bo.end){const n=Math.min(window.innerHeight,document.documentElement.clientHeight);if(i>n&&this.__oldViewportHeight){const r=this.__oldViewportHeight-n;a=i-r}this.__oldViewportHeight=n}if(o.end===wo.end){const n=Math.min(window.innerWidth,document.documentElement.clientWidth);if(i>n&&this.__oldViewportWidth){const r=this.__oldViewportWidth-n;a=i-r}this.__oldViewportWidth=n}}return a}__calculatePositionInOneDimension(e,o,i,a,n,r){const l=r?a.start:a.end,s=r?a.end:a.start,c=parseFloat(n.style[l]||getComputedStyle(n)[l]),d=this.__adjustBottomProperty(l,a,c),h=o[r?a.start:a.end]-e[i===r?a.end:a.start],m=d?`${d}px`:`${c+h*(r?-1:1)}px`;return{[l]:m,[s]:""}}};class ms extends CustomEvent{constructor(e){super("color-picker-change",{detail:{value:e}})}}const Ta=k`
  :host {
    --preview-size: 24px;
    --preview-color: rgba(0, 0, 0, 0);
  }

  .preview {
    --preview-bg-size: calc(var(--preview-size) / 2);
    --preview-bg-pos: calc(var(--preview-size) / 4);

    width: var(--preview-size);
    height: var(--preview-size);
    padding: 0;
    position: relative;
    overflow: hidden;
    background: none;
    border: solid 2px #888;
    border-radius: 4px;
    box-sizing: content-box;
  }

  .preview::before,
  .preview::after {
    position: absolute;
    top: 0;
    left: 0;
    width: 100%;
    height: 100%;
  }

  .preview::before {
    content: '';
    background: white;
    background-image: linear-gradient(45deg, #666 25%, transparent 25%),
      linear-gradient(45deg, transparent 75%, #666 75%), linear-gradient(45deg, transparent 75%, #666 75%),
      linear-gradient(45deg, #666 25%, transparent 25%);
    background-size: var(--preview-bg-size) var(--preview-bg-size);
    background-position: 0 0, 0 0, calc(var(--preview-bg-pos) * -1) calc(var(--preview-bg-pos) * -1),
      var(--preview-bg-pos) var(--preview-bg-pos);
  }

  .preview::after {
    content: '';
    background-color: var(--preview-color);
  }
`;let at=class extends j{constructor(){super(...arguments),this.commitValue=!1}static get styles(){return[Ta,k`
        #toggle {
          display: block;
        }
      `]}update(t){super.update(t),t.has("value")&&this.overlay&&this.overlay.requestContentUpdate()}firstUpdated(){this.overlay=document.createElement("vaadin-dev-tools-color-picker-overlay"),this.overlay.renderer=this.renderOverlayContent.bind(this),this.overlay.owner=this,this.overlay.positionTarget=this.toggle,this.overlay.noVerticalOverlap=!0,this.overlay.addEventListener("vaadin-overlay-escape-press",this.handleOverlayEscape.bind(this)),this.overlay.addEventListener("vaadin-overlay-close",this.handleOverlayClose.bind(this)),this.append(this.overlay)}render(){const t=this.value||"rgba(0, 0, 0, 0)";return y` <button
      id="toggle"
      class="preview"
      style="--preview-color: ${t}"
      @click=${this.open}
    ></button>`}open(){this.commitValue=!1,this.overlay.opened=!0,this.overlay.style.zIndex="1000000";const t=this.overlay.shadowRoot.querySelector('[part="overlay"]');t.style.background="#333"}renderOverlayContent(t){const e=getComputedStyle(this.toggle,"::after").getPropertyValue("background-color");Ee(y` <div>
        <vaadin-dev-tools-color-picker-overlay-content
          .value=${e}
          .presets=${this.presets}
          @color-changed=${this.handleColorChange.bind(this)}
        ></vaadin-dev-tools-color-picker-overlay-content>
      </div>`,t)}handleColorChange(t){this.commitValue=!0,this.dispatchEvent(new ms(t.detail.value)),t.detail.close&&(this.overlay.opened=!1,this.handleOverlayClose())}handleOverlayEscape(){this.commitValue=!1}handleOverlayClose(){const t=this.commitValue?"color-picker-commit":"color-picker-cancel";this.dispatchEvent(new CustomEvent(t))}};g([w({})],at.prototype,"value",void 0);g([w({})],at.prototype,"presets",void 0);g([st("#toggle")],at.prototype,"toggle",void 0);at=g([V("vaadin-dev-tools-color-picker")],at);let Ot=class extends j{static get styles(){return[Ta,k`
        :host {
          display: block;
          padding: 12px;
        }

        .picker::part(saturation),
        .picker::part(hue) {
          margin-bottom: 10px;
        }

        .picker::part(hue),
        .picker::part(alpha) {
          flex: 0 0 20px;
        }

        .picker::part(saturation),
        .picker::part(hue),
        .picker::part(alpha) {
          border-radius: 3px;
        }

        .picker::part(saturation-pointer),
        .picker::part(hue-pointer),
        .picker::part(alpha-pointer) {
          width: 20px;
          height: 20px;
        }

        .swatches {
          display: grid;
          grid-template-columns: repeat(6, var(--preview-size));
          grid-column-gap: 10px;
          grid-row-gap: 6px;
          margin-top: 16px;
        }
      `]}render(){return y` <div>
      <vaadin-dev-tools-rgba-string-color-picker
        class="picker"
        .color=${this.value}
        @color-changed=${this.handlePickerChange}
      ></vaadin-dev-tools-rgba-string-color-picker>
      ${this.renderSwatches()}
    </div>`}renderSwatches(){if(!this.presets||this.presets.length===0)return;const t=this.presets.map(e=>y` <button
        class="preview"
        style="--preview-color: ${e}"
        @click=${()=>this.selectPreset(e)}
      ></button>`);return y` <div class="swatches">${t}</div>`}handlePickerChange(t){this.dispatchEvent(new CustomEvent("color-changed",{detail:{value:t.detail.value}}))}selectPreset(t){this.dispatchEvent(new CustomEvent("color-changed",{detail:{value:t,close:!0}}))}};g([w({})],Ot.prototype,"value",void 0);g([w({})],Ot.prototype,"presets",void 0);Ot=g([V("vaadin-dev-tools-color-picker-overlay-content")],Ot);customElements.whenDefined("vaadin-overlay").then(()=>{const t=customElements.get("vaadin-overlay");class e extends ds(t){}customElements.define("vaadin-dev-tools-color-picker-overlay",e)});customElements.define("vaadin-dev-tools-rgba-string-color-picker",ls);let Ki=class extends K{constructor(){super(...arguments),this.presets=new At}static get styles(){return[K.styles,k`
        .editor-row {
          align-items: center;
        }

        .editor-row > .editor {
          display: flex;
          align-items: center;
          gap: 0.5rem;
        }
      `]}update(t){t.has("propertyMetadata")&&(this.presets=new At(this.propertyMetadata)),super.update(t)}renderEditor(){var t;return y`
      <vaadin-dev-tools-color-picker
        .value=${this.value}
        .presets=${this.presets.values}
        @color-picker-change=${this.handleColorPickerChange}
        @color-picker-commit=${this.handleColorPickerCommit}
        @color-picker-cancel=${this.handleColorPickerCancel}
      ></vaadin-dev-tools-color-picker>
      <vaadin-dev-tools-theme-text-input
        .value=${this.value}
        .showClearButton=${((t=this.propertyValue)==null?void 0:t.modified)||!1}
        @change=${this.handleInputChange}
      ></vaadin-dev-tools-theme-text-input>
    `}handleInputChange(t){this.value=t.detail.value,this.dispatchChange(this.value)}handleColorPickerChange(t){this.value=t.detail.value}handleColorPickerCommit(){this.dispatchChange(this.value)}handleColorPickerCancel(){this.updateValueFromTheme()}dispatchChange(t){const e=this.presets.tryMapToPreset(t);super.dispatchChange(e)}updateValueFromTheme(){var t;super.updateValueFromTheme(),this.value=this.presets.tryMapToRawValue(((t=this.propertyValue)==null?void 0:t.value)||"")}};Ki=g([V("vaadin-dev-tools-theme-color-property-editor")],Ki);class ps extends CustomEvent{constructor(e){super("open-css",{detail:{element:e}})}}let Lt=class extends j{static get styles(){return k`
      .section .header {
        display: flex;
        align-items: baseline;
        justify-content: space-between;
        padding: 0.4rem var(--theme-editor-section-horizontal-padding);
        color: var(--dev-tools-text-color-emphasis);
        background-color: rgba(0, 0, 0, 0.2);
      }

      .section .property-list .property-editor:not(:last-child) {
        border-bottom: solid 1px rgba(0, 0, 0, 0.2);
      }

      .section .header .open-css {
        all: initial;
        font-family: inherit;
        font-size: var(--dev-tools-font-size-small);
        line-height: 1;
        white-space: nowrap;
        background-color: rgba(255, 255, 255, 0.12);
        color: var(--dev-tools-text-color);
        font-weight: 600;
        padding: 0.25rem 0.375rem;
        border-radius: 0.25rem;
      }

      .section .header .open-css:hover {
        color: var(--dev-tools-text-color-emphasis);
      }
    `}render(){const t=this.metadata.elements.map(e=>this.renderSection(e));return y` <div>${t}</div> `}renderSection(t){const e=t.properties.map(o=>this.renderPropertyEditor(t,o));return y`
      <div class="section" data-testid=${t==null?void 0:t.displayName}>
        <div class="header">
          <span> ${t.displayName} </span>
          <button class="open-css" @click=${()=>this.handleOpenCss(t)}>Edit CSS</button>
        </div>
        <div class="property-list">${e}</div>
      </div>
    `}handleOpenCss(t){this.dispatchEvent(new ps(t))}renderPropertyEditor(t,e){let o;switch(e.editorType){case N.checkbox:o=gt`vaadin-dev-tools-theme-checkbox-property-editor`;break;case N.range:o=gt`vaadin-dev-tools-theme-range-property-editor`;break;case N.color:o=gt`vaadin-dev-tools-theme-color-property-editor`;break;default:o=gt`vaadin-dev-tools-theme-text-property-editor`}return xr` <${o}
          class="property-editor"
          .elementMetadata=${t}
          .propertyMetadata=${e}
          .theme=${this.theme}
          data-testid=${e.propertyName}
        >
        </${o}>`}};g([w({})],Lt.prototype,"metadata",void 0);g([w({})],Lt.prototype,"theme",void 0);Lt=g([V("vaadin-dev-tools-theme-property-list")],Lt);let Mt=class extends j{render(){return y`<div
      tabindex="-1"
      @mousemove=${this.onMouseMove}
      @click=${this.onClick}
      @keydown=${this.onKeyDown}
    ></div>`}onClick(t){const e=this.getTargetElement(t);this.dispatchEvent(new CustomEvent("shim-click",{detail:{target:e}}))}onMouseMove(t){const e=this.getTargetElement(t);this.dispatchEvent(new CustomEvent("shim-mousemove",{detail:{target:e}}))}onKeyDown(t){this.dispatchEvent(new CustomEvent("shim-keydown",{detail:{originalEvent:t}}))}getTargetElement(t){this.style.display="none";const e=document.elementFromPoint(t.clientX,t.clientY);return this.style.display="",e}};Mt.shadowRootOptions={...j.shadowRootOptions,delegatesFocus:!0};Mt.styles=[k`
      div {
        pointer-events: auto;
        background: rgba(255, 255, 255, 0);
        position: fixed;
        inset: 0px;
        z-index: 1000000;
      }
    `];Mt=g([V("vaadin-dev-tools-shim")],Mt);const $a=k`
  .popup {
    width: auto;
    position: fixed;
    background-color: var(--dev-tools-background-color-active-blurred);
    color: var(--dev-tools-text-color-primary);
    padding: 0.1875rem 0.75rem 0.1875rem 1rem;
    background-clip: padding-box;
    border-radius: var(--dev-tools-border-radius);
    overflow: hidden;
    margin: 0.5rem;
    width: 30rem;
    max-width: calc(100% - 1rem);
    max-height: calc(100vh - 1rem);
    flex-shrink: 1;
    background-color: var(--dev-tools-background-color-active);
    color: var(--dev-tools-text-color);
    transition: var(--dev-tools-transition-duration);
    transform-origin: bottom right;
    display: flex;
    flex-direction: column;
    box-shadow: var(--dev-tools-box-shadow);
    outline: none;
  }
`,us={resolve:t=>me(e=>e.classList.contains("cc-banner"),t)?document.querySelector("vaadin-cookie-consent"):void 0},hs={resolve:t=>{const e=me(o=>o.localName==="vaadin-login-overlay-wrapper",t);return e?e.__dataHost:void 0}},gs={resolve:t=>t.localName==="vaadin-dialog-overlay"?t.__dataHost:void 0},fs={resolve:t=>{const e=me(o=>o.localName==="vaadin-confirm-dialog-overlay",t);return e?e.__dataHost:void 0}},vs={resolve:t=>{const e=me(o=>o.localName==="vaadin-notification-card",t);return e?e.__dataHost:void 0}},xs={resolve:t=>t.localName!=="vaadin-menu-bar-item"?void 0:me(e=>e.localName==="vaadin-menu-bar",t)},Yi=[us,hs,gs,fs,vs,xs],ys={resolve:t=>me(e=>e.classList.contains("cc-banner"),t)},bs={resolve:t=>{var e;const o=me(i=>{var a;return((a=i.shadowRoot)==null?void 0:a.querySelector("[part=overlay]"))!=null},t);return(e=o==null?void 0:o.shadowRoot)==null?void 0:e.querySelector("[part=overlay]")}},ws={resolve:t=>{var e;const o=me(i=>i.localName==="vaadin-login-overlay-wrapper",t);return(e=o==null?void 0:o.shadowRoot)==null?void 0:e.querySelector("[part=card]")}},Ji=[ws,ys,bs],me=function(t,e){return t(e)?e:e.parentNode&&e.parentNode instanceof HTMLElement?me(t,e.parentNode):void 0};class _s{resolveElement(e){for(const o in Yi){let i=e;if((i=Yi[o].resolve(e))!==void 0)return i}return e}}class ks{resolveElement(e){for(const o in Ji){let i=e;if((i=Ji[o].resolve(e))!==void 0)return i}return e}}const Es=new _s,Ss=new ks;let xe=class extends j{constructor(){super(),this.active=!1,this.components=[],this.selected=0,this.mouseMoveEvent=this.mouseMoveEvent.bind(this)}connectedCallback(){super.connectedCallback();const t=new CSSStyleSheet;t.replaceSync(`
    .vaadin-dev-tools-highlight-overlay {
      pointer-events: none;
      position: absolute;
      z-index: 10000;
      background: rgba(158,44,198,0.25);
    }`),document.adoptedStyleSheets=[...document.adoptedStyleSheets,t],this.overlayElement=document.createElement("div"),this.overlayElement.classList.add("vaadin-dev-tools-highlight-overlay"),this.addEventListener("mousemove",this.mouseMoveEvent)}disconnectedCallback(){super.disconnectedCallback(),this.removeEventListener("mousemove",this.mouseMoveEvent)}render(){var t;return this.active?(this.style.display="block",y`
      <vaadin-dev-tools-shim
        @shim-click=${this.shimClick}
        @shim-mousemove=${this.shimMove}
        @shim-keydown=${this.shimKeydown}
      ></vaadin-dev-tools-shim>
      <div class="window popup component-picker-info">${(t=this.options)==null?void 0:t.infoTemplate}</div>
      <div class="window popup component-picker-components-info">
        <div>
          ${this.components.map((e,o)=>y`<div class=${o===this.selected?"selected":""}>
                ${e.element.tagName.toLowerCase()}
              </div>`)}
        </div>
      </div>
    `):(this.style.display="none",null)}open(t){this.options=t,this.active=!0,this.dispatchEvent(new CustomEvent("component-picker-opened",{}))}close(){this.active=!1,this.dispatchEvent(new CustomEvent("component-picker-closed",{}))}update(t){if(super.update(t),(t.has("selected")||t.has("components"))&&this.highlight(this.components[this.selected]),t.has("active")){const e=t.get("active"),o=this.active;!e&&o?requestAnimationFrame(()=>this.shim.focus()):e&&!o&&this.highlight(void 0)}}mouseMoveEvent(t){var e;if(!this.active){this.style.display="none";return}const o=(e=this.shadowRoot)==null?void 0:e.querySelector(".component-picker-info");if(o){const i=o.getBoundingClientRect();t.x>i.x&&t.x<i.x+i.width&&t.y>i.y&&t.y<=i.y+i.height?o.style.opacity="0.05":o.style.opacity="1.0"}}shimKeydown(t){const e=t.detail.originalEvent;if(e.key==="Escape")this.close(),t.stopPropagation(),t.preventDefault();else if(e.key==="ArrowUp"){let o=this.selected-1;o<0&&(o=this.components.length-1),this.selected=o}else e.key==="ArrowDown"?this.selected=(this.selected+1)%this.components.length:e.key==="Enter"&&(this.pickSelectedComponent(),t.stopPropagation(),t.preventDefault())}shimMove(t){const e=Es.resolveElement(t.detail.target);this.components=wr(e),this.selected=this.components.length-1,this.components[this.selected].highlightElement=Ss.resolveElement(t.detail.target)}shimClick(t){this.pickSelectedComponent()}pickSelectedComponent(){const t=this.components[this.selected];if(t&&this.options)try{this.options.pickCallback(t)}catch(e){console.error("Pick callback failed",e)}this.close()}highlight(t){let e=(t==null?void 0:t.highlightElement)??(t==null?void 0:t.element);if(this.highlighted!==e)if(e){const o=e.getBoundingClientRect(),i=getComputedStyle(e);this.overlayElement.style.top=`${o.top}px`,this.overlayElement.style.left=`${o.left}px`,this.overlayElement.style.width=`${o.width}px`,this.overlayElement.style.height=`${o.height}px`,this.overlayElement.style.borderRadius=i.borderRadius,document.body.append(this.overlayElement)}else this.overlayElement.remove();this.highlighted=e}};xe.styles=[$a,k`
      .component-picker-info {
        left: 1em;
        bottom: 1em;
      }

      .component-picker-components-info {
        right: 3em;
        bottom: 1em;
      }

      .component-picker-components-info .selected {
        font-weight: bold;
      }
    `];g([I()],xe.prototype,"active",void 0);g([I()],xe.prototype,"components",void 0);g([I()],xe.prototype,"selected",void 0);g([st("vaadin-dev-tools-shim")],xe.prototype,"shim",void 0);xe=g([V("vaadin-dev-tools-component-picker")],xe);const Cs=Object.freeze(Object.defineProperty({__proto__:null,get ComponentPicker(){return xe}},Symbol.toStringTag,{value:"Module"}));class zs{constructor(){this.currentActiveComponent=null,this.currentActiveComponentMetaData=null,this.componentPicked=async(e,o)=>{await this.hideOverlay(),this.currentActiveComponent=e,this.currentActiveComponentMetaData=o},this.showOverlay=()=>{!this.currentActiveComponent||!this.currentActiveComponentMetaData||this.currentActiveComponentMetaData.openOverlay&&this.currentActiveComponentMetaData.openOverlay(this.currentActiveComponent)},this.hideOverlay=()=>{!this.currentActiveComponent||!this.currentActiveComponentMetaData||this.currentActiveComponentMetaData.hideOverlay&&this.currentActiveComponentMetaData.hideOverlay(this.currentActiveComponent)},this.reset=()=>{this.currentActiveComponent=null,this.currentActiveComponentMetaData=null}}}const _e=new zs,Ll=t=>{const e=t.element.$.comboBox,o=e.$.overlay;Ts(t.element,e,o)},Ml=t=>{const e=t.element,o=e.$.comboBox,i=o.$.overlay;$s(e,o,i)},Ts=(t,e,o)=>{t.opened=!0,o._storedModeless=o.modeless,o.modeless=!0,document._themeEditorDocClickListener=js(t,e),document.addEventListener("click",document._themeEditorDocClickListener),e.removeEventListener("focusout",e._boundOnFocusout)},$s=(t,e,o)=>{t.opened=!1,!(!e||!o)&&(o.modeless=o._storedModeless,delete o._storedModeless,e.addEventListener("focusout",e._boundOnFocusout),document.removeEventListener("click",document._themeEditorDocClickListener),delete document._themeEditorDocClickListener)},js=(t,e)=>o=>{const i=o.target;i!=null&&(e.opened=!As(i,t))};function As(t,e){if(!t||!t.tagName)return!0;if(t.tagName.startsWith("VAADIN-DEV"))return!1;let o=t,i={nodeId:-1,uiId:-1,element:void 0};for(;o&&o.parentNode&&(i=To(o),i.nodeId===-1);)o=o.parentElement?o.parentElement:o.parentNode.host;const a=To(e);return!(i.nodeId!==-1&&a.nodeId===i.nodeId)}Fo(k`
  .vaadin-theme-editor-highlight {
    outline: solid 2px #9e2cc6;
    outline-offset: 3px;
  }
`);let de=class extends j{constructor(){super(...arguments),this.expanded=!1,this.themeEditorState=Ue.enabled,this.context=null,this.baseTheme=null,this.editedTheme=null,this.effectiveTheme=null,this.markedAsUsed=!1}static get styles(){return k`
      :host {
        animation: fade-in var(--dev-tools-transition-duration) ease-in;
        --theme-editor-section-horizontal-padding: 0.75rem;
        display: flex;
        flex-direction: column;
        max-height: 400px;
      }

      .notice {
        padding: var(--theme-editor-section-horizontal-padding);
      }

      .notice a {
        color: var(--dev-tools-text-color-emphasis);
      }

      .hint vaadin-icon {
        color: var(--dev-tools-green-color);
        font-size: var(--lumo-icon-size-m);
      }

      .hint {
        display: flex;
        align-items: center;
        gap: var(--theme-editor-section-horizontal-padding);
      }

      .header {
        flex: 0 0 auto;
        border-bottom: solid 1px rgba(0, 0, 0, 0.2);
      }

      .header .picker-row {
        padding: var(--theme-editor-section-horizontal-padding);
        display: flex;
        gap: 20px;
        align-items: center;
        justify-content: space-between;
      }

      .picker {
        flex: 1 1 0;
        min-width: 0;
        display: flex;
        align-items: center;
      }

      .picker button {
        min-width: 0;
        display: inline-flex;
        align-items: center;
        padding: 0;
        line-height: 20px;
        border: none;
        background: none;
        color: var(--dev-tools-text-color);
      }

      .picker button:not(:disabled):hover {
        color: var(--dev-tools-text-color-emphasis);
      }

      .picker svg,
      .picker .component-type {
        flex: 0 0 auto;
        margin-right: 4px;
      }

      .picker .instance-name {
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
        color: #e5a2fce5;
      }

      .picker .instance-name-quote {
        color: #e5a2fce5;
      }

      .picker .no-selection {
        font-style: italic;
      }

      .actions {
        display: flex;
        align-items: center;
        gap: 8px;
      }

      .property-list {
        flex: 1 1 auto;
        overflow-y: auto;
      }

      .link-button {
        all: initial;
        font-family: inherit;
        font-size: var(--dev-tools-font-size-small);
        line-height: 1;
        white-space: nowrap;
        color: inherit;
        font-weight: 600;
        text-decoration: underline;
      }

      .link-button:focus,
      .link-button:hover {
        color: var(--dev-tools-text-color-emphasis);
      }

      .icon-button {
        padding: 0;
        line-height: 0;
        border: none;
        background: none;
        color: var(--dev-tools-text-color);
      }

      .icon-button:disabled {
        opacity: 0.5;
      }

      .icon-button:not(:disabled):hover {
        color: var(--dev-tools-text-color-emphasis);
      }
    `}firstUpdated(){this.api=new Vr(this.connection),this.history=new Dr(this.api),this.historyActions=this.history.allowedActions,this.undoRedoListener=t=>{var e,o;const i=t.key==="Z"||t.key==="z";i&&(t.ctrlKey||t.metaKey)&&t.shiftKey?(e=this.historyActions)!=null&&e.allowRedo&&this.handleRedo():i&&(t.ctrlKey||t.metaKey)&&(o=this.historyActions)!=null&&o.allowUndo&&this.handleUndo()},document.addEventListener("vaadin-theme-updated",()=>{we.clear(),this.refreshTheme()}),document.addEventListener("keydown",this.undoRedoListener),this.dispatchEvent(new CustomEvent("before-open"))}update(t){var e,o;super.update(t),t.has("expanded")&&(this.expanded?(this.highlightElement((e=this.context)==null?void 0:e.component.element),_e.showOverlay()):(_e.hideOverlay(),this.removeElementHighlight((o=this.context)==null?void 0:o.component.element)))}disconnectedCallback(){var t;super.disconnectedCallback(),this.removeElementHighlight((t=this.context)==null?void 0:t.component.element),_e.hideOverlay(),_e.reset(),document.removeEventListener("keydown",this.undoRedoListener),this.dispatchEvent(new CustomEvent("after-close"))}render(){var t,e,o;return this.themeEditorState===Ue.missing_theme?this.renderMissingThemeNotice():y`
      <div class="header">
        <div class="picker-row">
          ${this.renderPicker()}
          <div class="actions">
            ${(t=this.context)!=null&&t.metadata?y` <vaadin-dev-tools-theme-scope-selector
                  .value=${this.context.scope}
                  .metadata=${this.context.metadata}
                  @scope-change=${this.handleScopeChange}
                ></vaadin-dev-tools-theme-scope-selector>`:null}
            <button
              class="icon-button"
              data-testid="undo"
              ?disabled=${!((e=this.historyActions)!=null&&e.allowUndo)}
              @click=${this.handleUndo}
            >
              ${kt.undo}
            </button>
            <button
              class="icon-button"
              data-testid="redo"
              ?disabled=${!((o=this.historyActions)!=null&&o.allowRedo)}
              @click=${this.handleRedo}
            >
              ${kt.redo}
            </button>
          </div>
        </div>
        ${this.renderLocalClassNameEditor()}
      </div>
      ${this.renderPropertyList()}
    `}renderMissingThemeNotice(){return y`
      <div class="notice">
        It looks like you have not set up an application theme yet. Theme editor requires an existing theme to work
        with. Please check our
        <a href="https://vaadin.com/docs/latest/styling/application-theme" target="_blank">documentation</a>
        on how to set up an application theme.
      </div>
    `}renderPropertyList(){if(!this.context)return null;if(!this.context.metadata){const t=this.context.component.element.localName;return y`
        <div class="notice">Styling <code>&lt;${t}&gt;</code> components is not supported at the moment.</div>
      `}if(this.context.scope===P.local&&!this.context.accessible){const t=this.context.metadata.displayName;return y`
        ${this.context.metadata.notAccessibleDescription&&this.context.scope===P.local?y`<div class="notice hint" style="padding-bottom: 0;">
              <vaadin-icon icon="vaadin:lightbulb"></vaadin-icon>
              <div>${this.context.metadata.notAccessibleDescription}</div>
            </div>`:""}
        <div class="notice">
          The selected ${t} cannot be styled locally. Currently, Theme Editor only supports styling
          instances that are assigned to a local variable, like so:
          <pre><code>Button saveButton = new Button("Save");</code></pre>
          If you want to modify the code so that it satisfies this requirement,
          <button class="link-button" @click=${this.handleShowComponent}>click here</button>
          to open it in your IDE. Alternatively you can choose to style all ${t}s by selecting "Global" from
          the scope dropdown above.
        </div>
      `}return y` ${this.context.metadata.description&&this.context.scope===P.local?y`<div class="notice hint">
            <vaadin-icon icon="vaadin:lightbulb"></vaadin-icon>
            <div>${this.context.metadata.description}</div>
          </div>`:""}
      <vaadin-dev-tools-theme-property-list
        class="property-list"
        .metadata=${this.context.metadata}
        .theme=${this.effectiveTheme}
        @theme-property-value-change=${this.handlePropertyChange}
        @open-css=${this.handleOpenCss}
      ></vaadin-dev-tools-theme-property-list>`}handleShowComponent(){if(!this.context)return;const t=this.context.component,e={nodeId:t.nodeId,uiId:t.uiId};this.connection.sendShowComponentCreateLocation(e)}async handleOpenCss(t){if(!this.context)return;await this.ensureLocalClassName();const e={themeScope:this.context.scope,localClassName:this.context.localClassName},o=qe(t.detail.element,e);await this.api.openCss(o)}renderPicker(){var t;let e;if((t=this.context)!=null&&t.metadata){const o=this.context.scope===P.local?this.context.metadata.displayName:`All ${this.context.metadata.displayName}s`,i=y`<span class="component-type">${o}</span>`,a=this.context.scope===P.local?Lr(this.context.component):null,n=a?y` <span class="instance-name-quote">"</span><span class="instance-name">${a}</span
            ><span class="instance-name-quote">"</span>`:null;e=y`${i} ${n}`}else e=y`<span class="no-selection">Pick an element to get started</span>`;return y`
      <div class="picker">
        <button @click=${this.pickComponent}>${kt.crosshair} ${e}</button>
      </div>
    `}renderLocalClassNameEditor(){var t;const e=((t=this.context)==null?void 0:t.scope)===P.local&&this.context.accessible;if(!this.context||!e)return null;const o=this.context.localClassName||this.context.suggestedClassName;return y` <vaadin-dev-tools-theme-class-name-editor
      .className=${o}
      @class-name-change=${this.handleClassNameChange}
    >
    </vaadin-dev-tools-theme-class-name-editor>`}async handleClassNameChange(t){if(!this.context)return;const e=this.context.localClassName,o=t.detail.value;if(e){const i=this.context.component.element;this.context.localClassName=o;const a=await this.api.setLocalClassName(this.context.component,o);this.historyActions=this.history.push(a.requestId,()=>we.previewLocalClassName(i,o),()=>we.previewLocalClassName(i,e))}else this.context={...this.context,suggestedClassName:o}}async pickComponent(){var t;_e.hideOverlay(),this.removeElementHighlight((t=this.context)==null?void 0:t.component.element),this.pickerProvider().open({infoTemplate:y`
        <div>
          <h3>Locate the component to style</h3>
          <p>Use the mouse cursor to highlight components in the UI.</p>
          <p>Use arrow down/up to cycle through and highlight specific components under the cursor.</p>
          <p>Click the primary mouse button to select the component.</p>
        </div>
      `,pickCallback:async e=>{var o;const i=await Nr.getMetadata(e);if(!i){this.context={component:e,scope:((o=this.context)==null?void 0:o.scope)||P.local},this.baseTheme=null,this.editedTheme=null,this.effectiveTheme=null;return}await _e.componentPicked(e,i),this.highlightElement(e.element),this.refreshComponentAndTheme(e,i),_e.showOverlay()}})}handleScopeChange(t){this.context&&this.refreshTheme({...this.context,scope:t.detail.value})}async handlePropertyChange(t){if(!this.context||!this.baseTheme||!this.editedTheme)return;const{element:e,property:o,value:i}=t.detail;this.editedTheme.updatePropertyValue(e.selector,o.propertyName,i,!0),this.effectiveTheme=ve.combine(this.baseTheme,this.editedTheme),await this.ensureLocalClassName();const a={themeScope:this.context.scope,localClassName:this.context.localClassName},n=Ir(e,a,o.propertyName,i);try{const r=await this.api.setCssRules([n]);this.historyActions=this.history.push(r.requestId);const l=Pr(n);we.add(l)}catch(r){console.error("Failed to update property value",r)}}async handleUndo(){this.historyActions=await this.history.undo(),await this.refreshComponentAndTheme()}async handleRedo(){this.historyActions=await this.history.redo(),await this.refreshComponentAndTheme()}async ensureLocalClassName(){if(!this.context||this.context.scope===P.global||this.context.localClassName)return;if(!this.context.localClassName&&!this.context.suggestedClassName)throw new Error("Cannot assign local class name for the component because it does not have a suggested class name");const t=this.context.component.element,e=this.context.suggestedClassName;this.context.localClassName=e;const o=await this.api.setLocalClassName(this.context.component,e);this.historyActions=this.history.push(o.requestId,()=>we.previewLocalClassName(t,e),()=>we.previewLocalClassName(t))}async refreshComponentAndTheme(t,e){var o,i,a;if(t=t||((o=this.context)==null?void 0:o.component),e=e||((i=this.context)==null?void 0:i.metadata),!t||!e)return;const n=await this.api.loadComponentMetadata(t);this.markedAsUsed||this.api.markAsUsed().then(()=>{this.markedAsUsed=!0}),we.previewLocalClassName(t.element,n.className),await this.refreshTheme({scope:((a=this.context)==null?void 0:a.scope)||P.local,metadata:e,component:t,localClassName:n.className,suggestedClassName:n.suggestedClassName,accessible:n.accessible})}async refreshTheme(t){const e=t||this.context;if(!e||!e.metadata)return;if(e.scope===P.local&&!e.accessible){this.context=e,this.baseTheme=null,this.editedTheme=null,this.effectiveTheme=null;return}let o=new ve(e.metadata);if(!(e.scope===P.local&&!e.localClassName)){const a={themeScope:e.scope,localClassName:e.localClassName},n=e.metadata.elements.map(l=>qe(l,a)),r=await this.api.loadRules(n);o=ve.fromServerRules(e.metadata,a,r.rules)}const i=await Or(e.metadata);this.context=e,this.baseTheme=i,this.editedTheme=o,this.effectiveTheme=ve.combine(i,this.editedTheme)}highlightElement(t){t&&t.classList.add("vaadin-theme-editor-highlight")}removeElementHighlight(t){t&&t.classList.remove("vaadin-theme-editor-highlight")}};g([w({})],de.prototype,"expanded",void 0);g([w({})],de.prototype,"themeEditorState",void 0);g([w({})],de.prototype,"pickerProvider",void 0);g([w({})],de.prototype,"connection",void 0);g([I()],de.prototype,"historyActions",void 0);g([I()],de.prototype,"context",void 0);g([I()],de.prototype,"effectiveTheme",void 0);de=g([V("vaadin-dev-tools-theme-editor")],de);const Wo=1e3,Go=(t,e)=>{const o=Array.from(t.querySelectorAll(e.join(", "))),i=Array.from(t.querySelectorAll("*")).filter(a=>a.shadowRoot).flatMap(a=>Go(a.shadowRoot,e));return[...o,...i]};let Xi=!1;const nt=(t,e)=>{Xi||(window.addEventListener("message",a=>{a.data==="validate-license"&&window.location.reload()},!1),Xi=!0);const o=t._overlayElement;if(o){if(o.shadowRoot){const a=o.shadowRoot.querySelector("slot:not([name])");if(a&&a.assignedElements().length>0){nt(a.assignedElements()[0],e);return}}nt(o,e);return}const i=e.messageHtml?e.messageHtml:`${e.message} <p>Component: ${e.product.name} ${e.product.version}</p>`.replace(/https:([^ ]*)/g,"<a href='https:$1'>https:$1</a>");t.isConnected&&(t.outerHTML=`<no-license style="display:flex;align-items:center;text-align:center;justify-content:center;"><div>${i}</div></no-license>`)},Je={},Qi={},Be={},ja={},ie=t=>`${t.name}_${t.version}`,Zi=t=>{const{cvdlName:e,version:o}=t.constructor,i={name:e,version:o},a=t.tagName.toLowerCase();Je[e]=Je[e]??[],Je[e].push(a);const n=Be[ie(i)];n&&setTimeout(()=>nt(t,n),Wo),Be[ie(i)]||ja[ie(i)]||Qi[ie(i)]||(Qi[ie(i)]=!0,window.Vaadin.devTools.checkLicense(i))},Rs=t=>{ja[ie(t)]=!0,console.debug("License check ok for",t)},Aa=t=>{const e=t.product.name;Be[ie(t.product)]=t,console.error("License check failed for",e);const o=Je[e];(o==null?void 0:o.length)>0&&Go(document,o).forEach(i=>{setTimeout(()=>nt(i,Be[ie(t.product)]),Wo)})},Ns=t=>{const e=t.message,o=t.product.name;t.messageHtml=`No license found. <a target=_blank onclick="javascript:window.open(this.href);return false;" href="${e}">Go here to start a trial or retrieve your license.</a>`,Be[ie(t.product)]=t,console.error("No license found when checking",o);const i=Je[o];(i==null?void 0:i.length)>0&&Go(document,i).forEach(a=>{setTimeout(()=>nt(a,Be[ie(t.product)]),Wo)})},Is=()=>{window.Vaadin.devTools.createdCvdlElements.forEach(t=>{Zi(t)}),window.Vaadin.devTools.createdCvdlElements={push:t=>{Zi(t)}}};var S;(function(t){t.ACTIVE="active",t.INACTIVE="inactive",t.UNAVAILABLE="unavailable",t.ERROR="error"})(S||(S={}));class Me extends Object{constructor(e){super(),this.status=S.UNAVAILABLE,e&&(this.webSocket=new WebSocket(e),this.webSocket.onmessage=o=>this.handleMessage(o),this.webSocket.onerror=o=>this.handleError(o),this.webSocket.onclose=o=>{this.status!==S.ERROR&&this.setStatus(S.UNAVAILABLE),this.webSocket=void 0}),setInterval(()=>{this.webSocket&&self.status!==S.ERROR&&this.status!==S.UNAVAILABLE&&this.webSocket.send("")},Me.HEARTBEAT_INTERVAL)}onHandshake(){}onReload(){}onUpdate(e,o){}onConnectionError(e){}onStatusChange(e){}onMessage(e){console.error("Unknown message received from the live reload server:",e)}handleMessage(e){let o;if(e.data!=="X"){try{o=JSON.parse(e.data)}catch(i){this.handleError(`[${i.name}: ${i.message}`);return}o.command==="hello"?(this.setStatus(S.ACTIVE),this.onHandshake()):o.command==="reload"?this.status===S.ACTIVE&&this.onReload():o.command==="update"?this.status===S.ACTIVE&&this.onUpdate(o.path,o.content):o.command==="license-check-ok"?Rs(o.data):o.command==="license-check-failed"?Aa(o.data):o.command==="license-check-nokey"?Ns(o.data):this.onMessage(o)}}handleError(e){console.error(e),this.setStatus(S.ERROR),e instanceof Event&&this.webSocket?this.onConnectionError(`Error in WebSocket connection to ${this.webSocket.url}`):this.onConnectionError(e)}setActive(e){!e&&this.status===S.ACTIVE?this.setStatus(S.INACTIVE):e&&this.status===S.INACTIVE&&this.setStatus(S.ACTIVE)}setStatus(e){this.status!==e&&(this.status=e,this.onStatusChange(e))}send(e,o){const i=JSON.stringify({command:e,data:o});this.webSocket?this.webSocket.readyState!==WebSocket.OPEN?this.webSocket.addEventListener("open",()=>this.webSocket.send(i)):this.webSocket.send(i):console.error(`Unable to send message ${e}. No websocket is available`)}setFeature(e,o){this.send("setFeature",{featureId:e,enabled:o})}sendTelemetry(e){this.send("reportTelemetry",{browserData:e})}sendLicenseCheck(e){this.send("checkLicense",e)}sendShowComponentCreateLocation(e){this.send("showComponentCreateLocation",e)}sendShowComponentAttachLocation(e){this.send("showComponentAttachLocation",e)}}Me.HEARTBEAT_INTERVAL=18e4;let No=class extends j{createRenderRoot(){return this}activate(){this._devTools.unreadErrors=!1,this.updateComplete.then(()=>{const t=this.renderRoot.querySelector(".message-tray .message:last-child");t&&t.scrollIntoView()})}render(){return y`<div class="message-tray">
      ${this._devTools.messages.map(t=>this._devTools.renderMessage(t))}
    </div>`}};g([w({type:Object})],No.prototype,"_devTools",void 0);No=g([V("vaadin-dev-tools-log")],No);var Ps=function(){var t=document.getSelection();if(!t.rangeCount)return function(){};for(var e=document.activeElement,o=[],i=0;i<t.rangeCount;i++)o.push(t.getRangeAt(i));switch(e.tagName.toUpperCase()){case"INPUT":case"TEXTAREA":e.blur();break;default:e=null;break}return t.removeAllRanges(),function(){t.type==="Caret"&&t.removeAllRanges(),t.rangeCount||o.forEach(function(a){t.addRange(a)}),e&&e.focus()}},ea={"text/plain":"Text","text/html":"Url",default:"Text"},Os="Copy to clipboard: #{key}, Enter";function Ls(t){var e=(/mac os x/i.test(navigator.userAgent)?"⌘":"Ctrl")+"+C";return t.replace(/#{\s*key\s*}/g,e)}function Ms(t,e){var o,i,a,n,r,l,s=!1;e||(e={}),o=e.debug||!1;try{a=Ps(),n=document.createRange(),r=document.getSelection(),l=document.createElement("span"),l.textContent=t,l.style.all="unset",l.style.position="fixed",l.style.top=0,l.style.clip="rect(0, 0, 0, 0)",l.style.whiteSpace="pre",l.style.webkitUserSelect="text",l.style.MozUserSelect="text",l.style.msUserSelect="text",l.style.userSelect="text",l.addEventListener("copy",function(d){if(d.stopPropagation(),e.format)if(d.preventDefault(),typeof d.clipboardData>"u"){o&&console.warn("unable to use e.clipboardData"),o&&console.warn("trying IE specific stuff"),window.clipboardData.clearData();var h=ea[e.format]||ea.default;window.clipboardData.setData(h,t)}else d.clipboardData.clearData(),d.clipboardData.setData(e.format,t);e.onCopy&&(d.preventDefault(),e.onCopy(d.clipboardData))}),document.body.appendChild(l),n.selectNodeContents(l),r.addRange(n);var c=document.execCommand("copy");if(!c)throw new Error("copy command was unsuccessful");s=!0}catch(d){o&&console.error("unable to copy using execCommand: ",d),o&&console.warn("trying IE specific stuff");try{window.clipboardData.setData(e.format||"text",t),e.onCopy&&e.onCopy(window.clipboardData),s=!0}catch(h){o&&console.error("unable to copy using clipboardData: ",h),o&&console.error("falling back to prompt"),i=Ls("message"in e?e.message:Os),window.prompt(i,t)}}finally{r&&(typeof r.removeRange=="function"?r.removeRange(n):r.removeAllRanges()),l&&document.body.removeChild(l),a()}return s}let Vt=class extends j{constructor(){super(...arguments),this.serverInfo={versions:[]}}createRenderRoot(){return this}render(){return y` <div class="info-tray">
      <button class="button copy" @click=${this.copyInfoToClipboard}>Copy</button>
      <dl>
        ${this.serverInfo.versions.map(t=>y`
            <dt>${t.name}</dt>
            <dd>${t.version}</dd>
          `)}
        <dt>Browser</dt>
        <dd>${navigator.userAgent}</dd>
        <dt>
          Live reload
          <label class="switch">
            <input
              id="toggle"
              type="checkbox"
              ?disabled=${this._devTools.liveReloadDisabled||(this._devTools.frontendStatus===S.UNAVAILABLE||this._devTools.frontendStatus===S.ERROR)&&(this._devTools.javaStatus===S.UNAVAILABLE||this._devTools.javaStatus===S.ERROR)}
              ?checked="${this._devTools.frontendStatus===S.ACTIVE||this._devTools.javaStatus===S.ACTIVE}"
              @change=${t=>this._devTools.setActive(t.target.checked)}
            />
            <span class="slider"></span>
          </label>
        </dt>
        <dd
          class="live-reload-status"
          style="--status-color: ${this._devTools.getStatusColor(this._devTools.javaStatus)}"
        >
          Java ${this._devTools.javaStatus}
          ${this._devTools.backend?`(${E.BACKEND_DISPLAY_NAME[this._devTools.backend]})`:""}
        </dd>
        <dd
          class="live-reload-status"
          style="--status-color: ${this._devTools.getStatusColor(this._devTools.frontendStatus)}"
        >
          Front end ${this._devTools.frontendStatus}
        </dd>
      </dl>
    </div>`}handleMessage(t){return(t==null?void 0:t.command)==="serverInfo"?(this.serverInfo=t.data,!0):!1}copyInfoToClipboard(){const t=this.renderRoot.querySelectorAll(".info-tray dt, .info-tray dd"),e=Array.from(t).map(o=>(o.localName==="dd"?": ":`
`)+o.textContent.trim()).join("").replace(/^\n/,"");Ms(e),this._devTools.showNotification(F.INFORMATION,"Environment information copied to clipboard",void 0,void 0,"versionInfoCopied")}};g([w({type:Object})],Vt.prototype,"_devTools",void 0);g([I()],Vt.prototype,"serverInfo",void 0);Vt=g([V("vaadin-dev-tools-info")],Vt);var z,F;(function(t){t.LOG="log",t.INFORMATION="information",t.WARNING="warning",t.ERROR="error"})(F||(F={}));let E=z=class extends j{static get styles(){return[k`
        :host {
          --dev-tools-font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Oxygen-Sans, Ubuntu, Cantarell,
            'Helvetica Neue', sans-serif;
          --dev-tools-font-family-monospace: SFMono-Regular, Menlo, Monaco, Consolas, 'Liberation Mono', 'Courier New',
            monospace;

          --dev-tools-font-size: 0.8125rem;
          --dev-tools-font-size-small: 0.75rem;

          --dev-tools-text-color: rgba(255, 255, 255, 0.8);
          --dev-tools-text-color-secondary: rgba(255, 255, 255, 0.65);
          --dev-tools-text-color-emphasis: rgba(255, 255, 255, 0.95);
          --dev-tools-text-color-active: rgba(255, 255, 255, 1);

          --dev-tools-background-color-inactive: rgba(45, 45, 45, 0.25);
          --dev-tools-background-color-active: rgba(45, 45, 45, 0.98);
          --dev-tools-background-color-active-blurred: rgba(45, 45, 45, 0.85);

          --dev-tools-border-radius: 0.5rem;
          --dev-tools-box-shadow: 0 0 0 1px rgba(255, 255, 255, 0.05), 0 4px 12px -2px rgba(0, 0, 0, 0.4);

          --dev-tools-blue-hsl: 206, 100%, 70%;
          --dev-tools-blue-color: hsl(var(--dev-tools-blue-hsl));
          --dev-tools-green-hsl: 145, 80%, 42%;
          --dev-tools-green-color: hsl(var(--dev-tools-green-hsl));
          --dev-tools-grey-hsl: 0, 0%, 50%;
          --dev-tools-grey-color: hsl(var(--dev-tools-grey-hsl));
          --dev-tools-yellow-hsl: 38, 98%, 64%;
          --dev-tools-yellow-color: hsl(var(--dev-tools-yellow-hsl));
          --dev-tools-red-hsl: 355, 100%, 68%;
          --dev-tools-red-color: hsl(var(--dev-tools-red-hsl));

          /* Needs to be in ms, used in JavaScript as well */
          --dev-tools-transition-duration: 180ms;

          all: initial;

          direction: ltr;
          cursor: default;
          font: normal 400 var(--dev-tools-font-size) / 1.125rem var(--dev-tools-font-family);
          color: var(--dev-tools-text-color);
          -webkit-user-select: none;
          -moz-user-select: none;
          user-select: none;
          color-scheme: dark;

          position: fixed;
          z-index: 20000;
          pointer-events: none;
          bottom: 0;
          right: 0;
          width: 100%;
          height: 100%;
          display: flex;
          flex-direction: column-reverse;
          align-items: flex-end;
        }

        .dev-tools {
          pointer-events: auto;
          display: flex;
          align-items: center;
          position: fixed;
          z-index: inherit;
          right: 0.5rem;
          bottom: 0.5rem;
          min-width: 1.75rem;
          height: 1.75rem;
          max-width: 1.75rem;
          border-radius: 0.5rem;
          padding: 0.375rem;
          box-sizing: border-box;
          background-color: var(--dev-tools-background-color-inactive);
          box-shadow: 0 0 0 1px rgba(255, 255, 255, 0.05);
          color: var(--dev-tools-text-color);
          transition: var(--dev-tools-transition-duration);
          white-space: nowrap;
          line-height: 1rem;
        }

        .dev-tools:hover,
        .dev-tools.active {
          background-color: var(--dev-tools-background-color-active);
          box-shadow: var(--dev-tools-box-shadow);
        }

        .dev-tools.active {
          max-width: calc(100% - 1rem);
        }

        .dev-tools .dev-tools-icon {
          flex: none;
          pointer-events: none;
          display: inline-block;
          width: 1rem;
          height: 1rem;
          fill: #fff;
          transition: var(--dev-tools-transition-duration);
          margin: 0;
        }

        .dev-tools.active .dev-tools-icon {
          opacity: 0;
          position: absolute;
          transform: scale(0.5);
        }

        .dev-tools .status-blip {
          flex: none;
          display: block;
          width: 6px;
          height: 6px;
          border-radius: 50%;
          z-index: 20001;
          background: var(--dev-tools-grey-color);
          position: absolute;
          top: -1px;
          right: -1px;
        }

        .dev-tools .status-description {
          overflow: hidden;
          text-overflow: ellipsis;
          padding: 0 0.25rem;
        }

        .dev-tools.error {
          background-color: hsla(var(--dev-tools-red-hsl), 0.15);
          animation: bounce 0.5s;
          animation-iteration-count: 2;
        }

        .switch {
          display: inline-flex;
          align-items: center;
        }

        .switch input {
          opacity: 0;
          width: 0;
          height: 0;
          position: absolute;
        }

        .switch .slider {
          display: block;
          flex: none;
          width: 28px;
          height: 18px;
          border-radius: 9px;
          background-color: rgba(255, 255, 255, 0.3);
          transition: var(--dev-tools-transition-duration);
          margin-right: 0.5rem;
        }

        .switch:focus-within .slider,
        .switch .slider:hover {
          background-color: rgba(255, 255, 255, 0.35);
          transition: none;
        }

        .switch input:focus-visible ~ .slider {
          box-shadow: 0 0 0 2px var(--dev-tools-background-color-active), 0 0 0 4px var(--dev-tools-blue-color);
        }

        .switch .slider::before {
          content: '';
          display: block;
          margin: 2px;
          width: 14px;
          height: 14px;
          background-color: #fff;
          transition: var(--dev-tools-transition-duration);
          border-radius: 50%;
        }

        .switch input:checked + .slider {
          background-color: var(--dev-tools-green-color);
        }

        .switch input:checked + .slider::before {
          transform: translateX(10px);
        }

        .switch input:disabled + .slider::before {
          background-color: var(--dev-tools-grey-color);
        }

        .window.hidden {
          opacity: 0;
          transform: scale(0);
          position: absolute;
        }

        .window.visible {
          transform: none;
          opacity: 1;
          pointer-events: auto;
        }

        .window.visible ~ .dev-tools {
          opacity: 0;
          pointer-events: none;
        }

        .window.visible ~ .dev-tools .dev-tools-icon,
        .window.visible ~ .dev-tools .status-blip {
          transition: none;
          opacity: 0;
        }

        .window {
          border-radius: var(--dev-tools-border-radius);
          overflow: auto;
          margin: 0.5rem;
          min-width: 30rem;
          max-width: calc(100% - 1rem);
          max-height: calc(100vh - 1rem);
          flex-shrink: 1;
          background-color: var(--dev-tools-background-color-active);
          color: var(--dev-tools-text-color);
          transition: var(--dev-tools-transition-duration);
          transform-origin: bottom right;
          display: flex;
          flex-direction: column;
          box-shadow: var(--dev-tools-box-shadow);
          outline: none;
        }

        .window-toolbar {
          display: flex;
          flex: none;
          align-items: center;
          padding: 0.375rem;
          white-space: nowrap;
          order: 1;
          background-color: rgba(0, 0, 0, 0.2);
          gap: 0.5rem;
        }

        .tab {
          color: var(--dev-tools-text-color-secondary);
          font: inherit;
          font-size: var(--dev-tools-font-size-small);
          font-weight: 500;
          line-height: 1;
          padding: 0.25rem 0.375rem;
          background: none;
          border: none;
          margin: 0;
          border-radius: 0.25rem;
          transition: var(--dev-tools-transition-duration);
        }

        .tab:hover,
        .tab.active {
          color: var(--dev-tools-text-color-active);
        }

        .tab.active {
          background-color: rgba(255, 255, 255, 0.12);
        }

        .tab.unreadErrors::after {
          content: '•';
          color: hsl(var(--dev-tools-red-hsl));
          font-size: 1.5rem;
          position: absolute;
          transform: translate(0, -50%);
        }

        .ahreflike {
          font-weight: 500;
          color: var(--dev-tools-text-color-secondary);
          text-decoration: underline;
          cursor: pointer;
        }

        .ahreflike:hover {
          color: var(--dev-tools-text-color-emphasis);
        }

        .button {
          all: initial;
          font-family: inherit;
          font-size: var(--dev-tools-font-size-small);
          line-height: 1;
          white-space: nowrap;
          background-color: rgba(0, 0, 0, 0.2);
          color: inherit;
          font-weight: 600;
          padding: 0.25rem 0.375rem;
          border-radius: 0.25rem;
        }

        .button:focus,
        .button:hover {
          color: var(--dev-tools-text-color-emphasis);
        }

        .minimize-button {
          flex: none;
          width: 1rem;
          height: 1rem;
          color: inherit;
          background-color: transparent;
          border: 0;
          padding: 0;
          margin: 0 0 0 auto;
          opacity: 0.8;
        }

        .minimize-button:hover {
          opacity: 1;
        }

        .minimize-button svg {
          max-width: 100%;
        }

        .message.information {
          --dev-tools-notification-color: var(--dev-tools-blue-color);
        }

        .message.warning {
          --dev-tools-notification-color: var(--dev-tools-yellow-color);
        }

        .message.error {
          --dev-tools-notification-color: var(--dev-tools-red-color);
        }

        .message {
          display: flex;
          padding: 0.1875rem 0.75rem 0.1875rem 2rem;
          background-clip: padding-box;
        }

        .message.log {
          padding-left: 0.75rem;
        }

        .message-content {
          margin-right: 0.5rem;
          -webkit-user-select: text;
          -moz-user-select: text;
          user-select: text;
        }

        .message-heading {
          position: relative;
          display: flex;
          align-items: center;
          margin: 0.125rem 0;
        }

        .message.log {
          color: var(--dev-tools-text-color-secondary);
        }

        .message:not(.log) .message-heading {
          font-weight: 500;
        }

        .message.has-details .message-heading {
          color: var(--dev-tools-text-color-emphasis);
          font-weight: 600;
        }

        .message-heading::before {
          position: absolute;
          margin-left: -1.5rem;
          display: inline-block;
          text-align: center;
          font-size: 0.875em;
          font-weight: 600;
          line-height: calc(1.25em - 2px);
          width: 14px;
          height: 14px;
          box-sizing: border-box;
          border: 1px solid transparent;
          border-radius: 50%;
        }

        .message.information .message-heading::before {
          content: 'i';
          border-color: currentColor;
          color: var(--dev-tools-notification-color);
        }

        .message.warning .message-heading::before,
        .message.error .message-heading::before {
          content: '!';
          color: var(--dev-tools-background-color-active);
          background-color: var(--dev-tools-notification-color);
        }

        .features-tray {
          padding: 0.75rem;
          flex: auto;
          overflow: auto;
          animation: fade-in var(--dev-tools-transition-duration) ease-in;
          user-select: text;
        }

        .features-tray p {
          margin-top: 0;
          color: var(--dev-tools-text-color-secondary);
        }

        .features-tray .feature {
          display: flex;
          align-items: center;
          gap: 1rem;
          padding-bottom: 0.5em;
        }

        .message .message-details {
          font-weight: 400;
          color: var(--dev-tools-text-color-secondary);
          margin: 0.25rem 0;
        }

        .message .message-details[hidden] {
          display: none;
        }

        .message .message-details p {
          display: inline;
          margin: 0;
          margin-right: 0.375em;
          word-break: break-word;
        }

        .message .persist {
          color: var(--dev-tools-text-color-secondary);
          white-space: nowrap;
          margin: 0.375rem 0;
          display: flex;
          align-items: center;
          position: relative;
          -webkit-user-select: none;
          -moz-user-select: none;
          user-select: none;
        }

        .message .persist::before {
          content: '';
          width: 1em;
          height: 1em;
          border-radius: 0.2em;
          margin-right: 0.375em;
          background-color: rgba(255, 255, 255, 0.3);
        }

        .message .persist:hover::before {
          background-color: rgba(255, 255, 255, 0.4);
        }

        .message .persist.on::before {
          background-color: rgba(255, 255, 255, 0.9);
        }

        .message .persist.on::after {
          content: '';
          order: -1;
          position: absolute;
          width: 0.75em;
          height: 0.25em;
          border: 2px solid var(--dev-tools-background-color-active);
          border-width: 0 0 2px 2px;
          transform: translate(0.05em, -0.05em) rotate(-45deg) scale(0.8, 0.9);
        }

        .message .dismiss-message {
          font-weight: 600;
          align-self: stretch;
          display: flex;
          align-items: center;
          padding: 0 0.25rem;
          margin-left: 0.5rem;
          color: var(--dev-tools-text-color-secondary);
        }

        .message .dismiss-message:hover {
          color: var(--dev-tools-text-color);
        }

        .notification-tray {
          display: flex;
          flex-direction: column-reverse;
          align-items: flex-end;
          margin: 0.5rem;
          flex: none;
        }

        .window.hidden + .notification-tray {
          margin-bottom: 3rem;
        }

        .notification-tray .message {
          pointer-events: auto;
          background-color: var(--dev-tools-background-color-active);
          color: var(--dev-tools-text-color);
          max-width: 30rem;
          box-sizing: border-box;
          border-radius: var(--dev-tools-border-radius);
          margin-top: 0.5rem;
          transition: var(--dev-tools-transition-duration);
          transform-origin: bottom right;
          animation: slideIn var(--dev-tools-transition-duration);
          box-shadow: var(--dev-tools-box-shadow);
          padding-top: 0.25rem;
          padding-bottom: 0.25rem;
        }

        .notification-tray .message.animate-out {
          animation: slideOut forwards var(--dev-tools-transition-duration);
        }

        .notification-tray .message .message-details {
          max-height: 10em;
          overflow: hidden;
        }

        .message-tray {
          flex: auto;
          overflow: auto;
          max-height: 20rem;
          user-select: text;
        }

        .message-tray .message {
          animation: fade-in var(--dev-tools-transition-duration) ease-in;
          padding-left: 2.25rem;
        }

        .message-tray .message.warning {
          background-color: hsla(var(--dev-tools-yellow-hsl), 0.09);
        }

        .message-tray .message.error {
          background-color: hsla(var(--dev-tools-red-hsl), 0.09);
        }

        .message-tray .message.error .message-heading {
          color: hsl(var(--dev-tools-red-hsl));
        }

        .message-tray .message.warning .message-heading {
          color: hsl(var(--dev-tools-yellow-hsl));
        }

        .message-tray .message + .message {
          border-top: 1px solid rgba(255, 255, 255, 0.07);
        }

        .message-tray .dismiss-message,
        .message-tray .persist {
          display: none;
        }

        .info-tray {
          padding: 0.75rem;
          position: relative;
          flex: auto;
          overflow: auto;
          animation: fade-in var(--dev-tools-transition-duration) ease-in;
          user-select: text;
        }

        .info-tray dl {
          margin: 0;
          display: grid;
          grid-template-columns: max-content 1fr;
          column-gap: 0.75rem;
          position: relative;
        }

        .info-tray dt {
          grid-column: 1;
          color: var(--dev-tools-text-color-emphasis);
        }

        .info-tray dt:not(:first-child)::before {
          content: '';
          width: 100%;
          position: absolute;
          height: 1px;
          background-color: rgba(255, 255, 255, 0.1);
          margin-top: -0.375rem;
        }

        .info-tray dd {
          grid-column: 2;
          margin: 0;
        }

        .info-tray :is(dt, dd):not(:last-child) {
          margin-bottom: 0.75rem;
        }

        .info-tray dd + dd {
          margin-top: -0.5rem;
        }

        .info-tray .live-reload-status::before {
          content: '•';
          color: var(--status-color);
          width: 0.75rem;
          display: inline-block;
          font-size: 1rem;
          line-height: 0.5rem;
        }

        .info-tray .copy {
          position: fixed;
          z-index: 1;
          top: 0.5rem;
          right: 0.5rem;
        }

        .info-tray .switch {
          vertical-align: -4px;
        }

        @keyframes slideIn {
          from {
            transform: translateX(100%);
            opacity: 0;
          }
          to {
            transform: translateX(0%);
            opacity: 1;
          }
        }

        @keyframes slideOut {
          from {
            transform: translateX(0%);
            opacity: 1;
          }
          to {
            transform: translateX(100%);
            opacity: 0;
          }
        }

        @keyframes fade-in {
          0% {
            opacity: 0;
          }
        }

        @keyframes bounce {
          0% {
            transform: scale(0.8);
          }
          50% {
            transform: scale(1.5);
            background-color: hsla(var(--dev-tools-red-hsl), 1);
          }
          100% {
            transform: scale(1);
          }
        }

        @supports (backdrop-filter: blur(1px)) {
          .dev-tools,
          .window,
          .notification-tray .message {
            backdrop-filter: blur(8px);
          }
          .dev-tools:hover,
          .dev-tools.active,
          .window,
          .notification-tray .message {
            background-color: var(--dev-tools-background-color-active-blurred);
          }
        }
      `,$a]}static get isActive(){const t=window.sessionStorage.getItem(z.ACTIVE_KEY_IN_SESSION_STORAGE);return t===null||t!=="false"}static notificationDismissed(t){const e=window.localStorage.getItem(z.DISMISSED_NOTIFICATIONS_IN_LOCAL_STORAGE);return e!==null&&e.includes(t)}elementTelemetry(){let t={};try{const e=localStorage.getItem("vaadin.statistics.basket");if(!e)return;t=JSON.parse(e)}catch{return}this.frontendConnection&&this.frontendConnection.sendTelemetry(t)}openWebSocketConnection(){this.frontendStatus=S.UNAVAILABLE,this.javaStatus=S.UNAVAILABLE;const t=l=>this.log(F.ERROR,l),e=()=>{this.showSplashMessage("Reloading…");const l=window.sessionStorage.getItem(z.TRIGGERED_COUNT_KEY_IN_SESSION_STORAGE),s=l?parseInt(l,10)+1:1;window.sessionStorage.setItem(z.TRIGGERED_COUNT_KEY_IN_SESSION_STORAGE,s.toString()),window.sessionStorage.setItem(z.TRIGGERED_KEY_IN_SESSION_STORAGE,"true"),window.location.reload()},o=(l,s)=>{let c=document.head.querySelector(`style[data-file-path='${l}']`);c?(this.log(F.INFORMATION,"Hot update of "+l),c.textContent=s,document.dispatchEvent(new CustomEvent("vaadin-theme-updated"))):e()},i=new Me(this.getDedicatedWebSocketUrl());i.onHandshake=()=>{this.log(F.LOG,"Vaadin development mode initialized"),z.isActive||i.setActive(!1),this.elementTelemetry()},i.onConnectionError=t,i.onReload=e,i.onUpdate=o,i.onStatusChange=l=>{this.frontendStatus=l},i.onMessage=l=>this.handleFrontendMessage(l),this.frontendConnection=i;let a;this.backend===z.SPRING_BOOT_DEVTOOLS&&this.springBootLiveReloadPort?(a=new Me(this.getSpringBootWebSocketUrl(window.location)),a.onHandshake=()=>{z.isActive||a.setActive(!1)},a.onReload=e,a.onConnectionError=t):this.backend===z.JREBEL||this.backend===z.HOTSWAP_AGENT?a=i:a=new Me(void 0);const n=a.onStatusChange;a.onStatusChange=l=>{n(l),this.javaStatus=l};const r=a.onHandshake;a.onHandshake=()=>{r(),this.backend&&this.log(F.INFORMATION,`Java live reload available: ${z.BACKEND_DISPLAY_NAME[this.backend]}`)},this.javaConnection=a,this.backend||this.showNotification(F.WARNING,"Java live reload unavailable","Live reload for Java changes is currently not set up. Find out how to make use of this functionality to boost your workflow.","https://vaadin.com/docs/latest/flow/configuration/live-reload","liveReloadUnavailable")}tabHandleMessage(t,e){const o=t;return o.handleMessage&&o.handleMessage.call(t,e)}handleFrontendMessage(t){for(const e of this.tabs)if(e.element&&this.tabHandleMessage(e.element,t))return;if((t==null?void 0:t.command)==="featureFlags")this.features=t.data.features;else if((t==null?void 0:t.command)==="themeEditorState"){const e=!!window.Vaadin.Flow;this.themeEditorState=t.data,e&&this.themeEditorState!==Ue.disabled&&(this.tabs.push({id:"theme-editor",title:"Theme Editor (Preview)",render:()=>this.renderThemeEditor()}),this.requestUpdate())}else this.unhandledMessages.push(t)}getDedicatedWebSocketUrl(){function t(o){const i=document.createElement("div");return i.innerHTML=`<a href="${o}"/>`,i.firstChild.href}if(this.url===void 0)return;const e=t(this.url);if(!e.startsWith("http://")&&!e.startsWith("https://")){console.error("The protocol of the url should be http or https for live reload to work.");return}return`${e.replace(/^http/,"ws")}?v-r=push&debug_window`}getSpringBootWebSocketUrl(t){const{hostname:e}=t,o=t.protocol==="https:"?"wss":"ws";if(e.endsWith("gitpod.io")){const i=e.replace(/.*?-/,"");return`${o}://${this.springBootLiveReloadPort}-${i}`}else return`${o}://${e}:${this.springBootLiveReloadPort}`}constructor(){super(),this.unhandledMessages=[],this.expanded=!1,this.messages=[],this.notifications=[],this.frontendStatus=S.UNAVAILABLE,this.javaStatus=S.UNAVAILABLE,this.tabs=[{id:"log",title:"Log",render:"vaadin-dev-tools-log"},{id:"info",title:"Info",render:"vaadin-dev-tools-info"},{id:"features",title:"Feature Flags",render:()=>this.renderFeatures()}],this.activeTab="log",this.features=[],this.unreadErrors=!1,this.componentPickActive=!1,this.themeEditorState=Ue.disabled,this.nextMessageId=1,this.transitionDuration=0,window.Vaadin.Flow&&this.tabs.push({id:"code",title:"Code",render:()=>this.renderCode()})}connectedCallback(){if(super.connectedCallback(),this.catchErrors(),this.disableEventListener=o=>this.demoteSplashMessage(),document.body.addEventListener("focus",this.disableEventListener),document.body.addEventListener("click",this.disableEventListener),window.sessionStorage.getItem(z.TRIGGERED_KEY_IN_SESSION_STORAGE)){const o=new Date,i=`${`0${o.getHours()}`.slice(-2)}:${`0${o.getMinutes()}`.slice(-2)}:${`0${o.getSeconds()}`.slice(-2)}`;this.showSplashMessage(`Page reloaded at ${i}`),window.sessionStorage.removeItem(z.TRIGGERED_KEY_IN_SESSION_STORAGE)}this.transitionDuration=parseInt(window.getComputedStyle(this).getPropertyValue("--dev-tools-transition-duration"),10);const t=window;t.Vaadin=t.Vaadin||{},t.Vaadin.devTools=Object.assign(this,t.Vaadin.devTools),document.documentElement.addEventListener("vaadin-overlay-outside-click",o=>{const i=o,a=i.target.owner;a&&_r(this,a)||i.detail.sourceEvent.composedPath().includes(this)&&o.preventDefault()});const e=window.Vaadin;e.devToolsPlugins&&(Array.from(e.devToolsPlugins).forEach(o=>this.initPlugin(o)),e.devToolsPlugins={push:o=>this.initPlugin(o)}),this.openWebSocketConnection(),Is()}async initPlugin(t){const e=this;t.init({addTab:(o,i)=>{e.tabs.push({id:o,title:o,render:i})},send:function(o,i){e.frontendConnection.send(o,i)}})}format(t){return t.toString()}catchErrors(){const t=window.Vaadin.ConsoleErrors;t&&t.forEach(e=>{this.log(F.ERROR,e.map(o=>this.format(o)).join(" "))}),window.Vaadin.ConsoleErrors={push:e=>{this.log(F.ERROR,e.map(o=>this.format(o)).join(" "))}}}disconnectedCallback(){this.disableEventListener&&(document.body.removeEventListener("focus",this.disableEventListener),document.body.removeEventListener("click",this.disableEventListener)),super.disconnectedCallback()}toggleExpanded(){this.notifications.slice().forEach(t=>this.dismissNotification(t.id)),this.expanded=!this.expanded,this.expanded&&this.root.focus()}showSplashMessage(t){this.splashMessage=t,this.splashMessage&&(this.expanded?this.demoteSplashMessage():setTimeout(()=>{this.demoteSplashMessage()},z.AUTO_DEMOTE_NOTIFICATION_DELAY))}demoteSplashMessage(){this.splashMessage&&this.log(F.LOG,this.splashMessage),this.showSplashMessage(void 0)}checkLicense(t){this.frontendConnection?this.frontendConnection.sendLicenseCheck(t):Aa({message:"Internal error: no connection",product:t})}log(t,e,o,i){const a=this.nextMessageId;for(this.nextMessageId+=1,this.messages.push({id:a,type:t,message:e,details:o,link:i,dontShowAgain:!1,deleted:!1});this.messages.length>z.MAX_LOG_ROWS;)this.messages.shift();this.requestUpdate(),this.updateComplete.then(()=>{const n=this.renderRoot.querySelector(".message-tray .message:last-child");this.expanded&&n?(setTimeout(()=>n.scrollIntoView({behavior:"smooth"}),this.transitionDuration),this.unreadErrors=!1):t===F.ERROR&&(this.unreadErrors=!0)})}showNotification(t,e,o,i,a){if(a===void 0||!z.notificationDismissed(a)){if(this.notifications.filter(r=>r.persistentId===a).filter(r=>!r.deleted).length>0)return;const n=this.nextMessageId;this.nextMessageId+=1,this.notifications.push({id:n,type:t,message:e,details:o,link:i,persistentId:a,dontShowAgain:!1,deleted:!1}),i===void 0&&setTimeout(()=>{this.dismissNotification(n)},z.AUTO_DEMOTE_NOTIFICATION_DELAY),this.requestUpdate()}else this.log(t,e,o,i)}dismissNotification(t){const e=this.findNotificationIndex(t);if(e!==-1&&!this.notifications[e].deleted){const o=this.notifications[e];if(o.dontShowAgain&&o.persistentId&&!z.notificationDismissed(o.persistentId)){let i=window.localStorage.getItem(z.DISMISSED_NOTIFICATIONS_IN_LOCAL_STORAGE);i=i===null?o.persistentId:`${i},${o.persistentId}`,window.localStorage.setItem(z.DISMISSED_NOTIFICATIONS_IN_LOCAL_STORAGE,i)}o.deleted=!0,this.log(o.type,o.message,o.details,o.link),setTimeout(()=>{const i=this.findNotificationIndex(t);i!==-1&&(this.notifications.splice(i,1),this.requestUpdate())},this.transitionDuration)}}findNotificationIndex(t){let e=-1;return this.notifications.some((o,i)=>o.id===t?(e=i,!0):!1),e}toggleDontShowAgain(t){const e=this.findNotificationIndex(t);if(e!==-1&&!this.notifications[e].deleted){const o=this.notifications[e];o.dontShowAgain=!o.dontShowAgain,this.requestUpdate()}}setActive(t){var e,o;(e=this.frontendConnection)==null||e.setActive(t),(o=this.javaConnection)==null||o.setActive(t),window.sessionStorage.setItem(z.ACTIVE_KEY_IN_SESSION_STORAGE,t?"true":"false")}getStatusColor(t){return t===S.ACTIVE?"var(--dev-tools-green-color)":t===S.INACTIVE?"var(--dev-tools-grey-color)":t===S.UNAVAILABLE?"var(--dev-tools-yellow-hsl)":t===S.ERROR?"var(--dev-tools-red-color)":"none"}renderMessage(t){return y`
      <div
        class="message ${t.type} ${t.deleted?"animate-out":""} ${t.details||t.link?"has-details":""}"
      >
        <div class="message-content">
          <div class="message-heading">${t.message}</div>
          <div class="message-details" ?hidden="${!t.details&&!t.link}">
            ${t.details?y`<p>${t.details}</p>`:""}
            ${t.link?y`<a class="ahreflike" href="${t.link}" target="_blank">Learn more</a>`:""}
          </div>
          ${t.persistentId?y`<div
                class="persist ${t.dontShowAgain?"on":"off"}"
                @click=${()=>this.toggleDontShowAgain(t.id)}
              >
                Don’t show again
              </div>`:""}
        </div>
        <div class="dismiss-message" @click=${()=>this.dismissNotification(t.id)}>Dismiss</div>
      </div>
    `}render(){return y` <div
        class="window ${this.expanded&&!this.componentPickActive?"visible":"hidden"}"
        tabindex="0"
        @keydown=${t=>t.key==="Escape"&&this.expanded&&this.toggleExpanded()}
      >
        <div class="window-toolbar">
          ${this.tabs.map(t=>y`<button
                class=${qo({tab:!0,active:this.activeTab===t.id,unreadErrors:t.id==="log"&&this.unreadErrors})}
                id="${t.id}"
                @click=${()=>{const e=this.tabs.find(a=>a.id===this.activeTab);if(e&&e.element){const a=typeof e.render=="function"?e.element.firstElementChild:e.element,n=a==null?void 0:a.deactivate;n&&n.call(a)}this.activeTab=t.id;const o=typeof t.render=="function"?t.element.firstElementChild:t.element,i=o.activate;i&&i.call(o)}}
              >
                ${t.title}
              </button> `)}
          <button class="minimize-button" title="Minimize" @click=${()=>this.toggleExpanded()}>
            <svg fill="none" height="16" viewBox="0 0 16 16" width="16" xmlns="http://www.w3.org/2000/svg">
              <g fill="#fff" opacity=".8">
                <path
                  d="m7.25 1.75c0-.41421.33579-.75.75-.75h3.25c2.0711 0 3.75 1.67893 3.75 3.75v6.5c0 2.0711-1.6789 3.75-3.75 3.75h-6.5c-2.07107 0-3.75-1.6789-3.75-3.75v-3.25c0-.41421.33579-.75.75-.75s.75.33579.75.75v3.25c0 1.2426 1.00736 2.25 2.25 2.25h6.5c1.2426 0 2.25-1.0074 2.25-2.25v-6.5c0-1.24264-1.0074-2.25-2.25-2.25h-3.25c-.41421 0-.75-.33579-.75-.75z"
                />
                <path
                  d="m2.96967 2.96967c.29289-.29289.76777-.29289 1.06066 0l5.46967 5.46967v-2.68934c0-.41421.33579-.75.75-.75.4142 0 .75.33579.75.75v4.5c0 .4142-.3358.75-.75.75h-4.5c-.41421 0-.75-.3358-.75-.75 0-.41421.33579-.75.75-.75h2.68934l-5.46967-5.46967c-.29289-.29289-.29289-.76777 0-1.06066z"
                />
              </g>
            </svg>
          </button>
        </div>
        <div id="tabContainer"></div>
      </div>

      <div class="notification-tray">${this.notifications.map(t=>this.renderMessage(t))}</div>
      <vaadin-dev-tools-component-picker
        .active=${this.componentPickActive}
        @component-picker-opened=${()=>{this.componentPickActive=!0}}
        @component-picker-closed=${()=>{this.componentPickActive=!1}}
      ></vaadin-dev-tools-component-picker>
      <div
        class="dev-tools ${this.splashMessage?"active":""}${this.unreadErrors?" error":""}"
        @click=${()=>this.toggleExpanded()}
      >
        ${this.unreadErrors?y`<svg
              fill="none"
              height="16"
              viewBox="0 0 16 16"
              width="16"
              xmlns="http://www.w3.org/2000/svg"
              xmlns:xlink="http://www.w3.org/1999/xlink"
              class="dev-tools-icon error"
            >
              <clipPath id="a"><path d="m0 0h16v16h-16z" /></clipPath>
              <g clip-path="url(#a)">
                <path
                  d="m6.25685 2.09894c.76461-1.359306 2.72169-1.359308 3.4863 0l5.58035 9.92056c.7499 1.3332-.2135 2.9805-1.7432 2.9805h-11.1606c-1.529658 0-2.4930857-1.6473-1.743156-2.9805z"
                  fill="#ff5c69"
                />
                <path
                  d="m7.99699 4c-.45693 0-.82368.37726-.81077.834l.09533 3.37352c.01094.38726.32803.69551.71544.69551.38741 0 .70449-.30825.71544-.69551l.09533-3.37352c.0129-.45674-.35384-.834-.81077-.834zm.00301 8c.60843 0 1-.3879 1-.979 0-.5972-.39157-.9851-1-.9851s-1 .3879-1 .9851c0 .5911.39157.979 1 .979z"
                  fill="#fff"
                />
              </g>
            </svg>`:y`<svg
              fill="none"
              height="17"
              viewBox="0 0 16 17"
              width="16"
              xmlns="http://www.w3.org/2000/svg"
              class="dev-tools-icon logo"
            >
              <g fill="#fff">
                <path
                  d="m8.88273 5.97926c0 .04401-.0032.08898-.00801.12913-.02467.42848-.37813.76767-.8117.76767-.43358 0-.78704-.34112-.81171-.76928-.00481-.04015-.00801-.08351-.00801-.12752 0-.42784-.10255-.87656-1.14434-.87656h-3.48364c-1.57118 0-2.315271-.72849-2.315271-2.21758v-1.26683c0-.42431.324618-.768314.748261-.768314.42331 0 .74441.344004.74441.768314v.42784c0 .47924.39576.81265 1.11293.81265h3.41538c1.5542 0 1.67373 1.156 1.725 1.7679h.03429c.05095-.6119.17048-1.7679 1.72468-1.7679h3.4154c.7172 0 1.0145-.32924 1.0145-.80847l-.0067-.43202c0-.42431.3227-.768314.7463-.768314.4234 0 .7255.344004.7255.768314v1.26683c0 1.48909-.6181 2.21758-2.1893 2.21758h-3.4836c-1.04182 0-1.14437.44872-1.14437.87656z"
                />
                <path
                  d="m8.82577 15.1648c-.14311.3144-.4588.5335-.82635.5335-.37268 0-.69252-.2249-.83244-.5466-.00206-.0037-.00412-.0073-.00617-.0108-.00275-.0047-.00549-.0094-.00824-.0145l-3.16998-5.87318c-.08773-.15366-.13383-.32816-.13383-.50395 0-.56168.45592-1.01879 1.01621-1.01879.45048 0 .75656.22069.96595.6993l2.16882 4.05042 2.17166-4.05524c.2069-.47379.513-.69448.9634-.69448.5603 0 1.0166.45711 1.0166 1.01879 0 .17579-.0465.35029-.1348.50523l-3.1697 5.8725c-.00503.0096-.01006.0184-.01509.0272-.00201.0036-.00402.0071-.00604.0106z"
                />
              </g>
            </svg>`}

        <span
          class="status-blip"
          style="background: linear-gradient(to right, ${this.getStatusColor(this.frontendStatus)} 50%, ${this.getStatusColor(this.javaStatus)} 50%)"
        ></span>
        ${this.splashMessage?y`<span class="status-description">${this.splashMessage}</span></div>`:$}
      </div>`}updated(t){var e;super.updated(t);const o=this.renderRoot.querySelector("#tabContainer"),i=[];if(this.tabs.forEach(n=>{n.element||(typeof n.render=="function"?n.element=document.createElement("div"):(n.element=document.createElement(n.render),n.element._devTools=this),i.push(n.element))}),(o==null?void 0:o.childElementCount)!==this.tabs.length){for(let n=0;n<this.tabs.length;n++){const r=this.tabs[n];o.childElementCount>n&&o.children[n]===r.element||o.insertBefore(r.element,o.children[n])}for(;(o==null?void 0:o.childElementCount)>this.tabs.length;)(e=o.lastElementChild)==null||e.remove()}for(const n of this.tabs){typeof n.render=="function"?Ee(n.render(),n.element):n.element.requestUpdate&&n.element.requestUpdate();const r=n.id===this.activeTab;n.element.hidden=!r}for(const n of i)for(var a=0;a<this.unhandledMessages.length;a++)this.tabHandleMessage(n,this.unhandledMessages[a])&&(this.unhandledMessages.splice(a,1),a--)}renderCode(){return y`<div class="info-tray">
      <div>
        <select id="locationType">
          <option value="create" selected>Create</option>
          <option value="attach">Attach</option>
        </select>
        <button
          class="button pick"
          @click=${async()=>{await f(()=>Promise.resolve().then(()=>Cs),void 0),this.componentPicker.open({infoTemplate:y`
                <div>
                  <h3>Locate a component in source code</h3>
                  <p>Use the mouse cursor to highlight components in the UI.</p>
                  <p>Use arrow down/up to cycle through and highlight specific components under the cursor.</p>
                  <p>
                    Click the primary mouse button to open the corresponding source code line of the highlighted
                    component in your IDE.
                  </p>
                </div>
              `,pickCallback:t=>{const e={nodeId:t.nodeId,uiId:t.uiId};this.renderRoot.querySelector("#locationType").value==="create"?this.frontendConnection.sendShowComponentCreateLocation(e):this.frontendConnection.sendShowComponentAttachLocation(e)}})}}
        >
          Find component in code
        </button>
      </div>
      </div>
    </div>`}renderFeatures(){return y`<div class="features-tray">
      ${this.features.map(t=>y`<div class="feature">
          <label class="switch">
            <input
              class="feature-toggle"
              id="feature-toggle-${t.id}"
              type="checkbox"
              ?checked=${t.enabled}
              @change=${e=>this.toggleFeatureFlag(e,t)}
            />
            <span class="slider"></span>
            ${t.title}
          </label>
          <a class="ahreflike" href="${t.moreInfoLink}" target="_blank">Learn more</a>
        </div>`)}
    </div>`}disableJavaLiveReload(){var t;(t=this.javaConnection)==null||t.setActive(!1)}enableJavaLiveReload(){var t;(t=this.javaConnection)==null||t.setActive(!0)}renderThemeEditor(){return y` <vaadin-dev-tools-theme-editor
      .expanded=${this.expanded}
      .themeEditorState=${this.themeEditorState}
      .pickerProvider=${()=>this.componentPicker}
      .connection=${this.frontendConnection}
      @before-open=${this.disableJavaLiveReload}
      @after-close=${this.enableJavaLiveReload}
    ></vaadin-dev-tools-theme-editor>`}toggleFeatureFlag(t,e){const o=t.target.checked;this.frontendConnection?(this.frontendConnection.setFeature(e.id,o),this.showNotification(F.INFORMATION,`“${e.title}” ${o?"enabled":"disabled"}`,e.requiresServerRestart?"This feature requires a server restart":void 0,void 0,`feature${e.id}${o?"Enabled":"Disabled"}`)):this.log(F.ERROR,`Unable to toggle feature ${e.title}: No server connection available`)}};E.MAX_LOG_ROWS=1e3;E.DISMISSED_NOTIFICATIONS_IN_LOCAL_STORAGE="vaadin.live-reload.dismissedNotifications";E.ACTIVE_KEY_IN_SESSION_STORAGE="vaadin.live-reload.active";E.TRIGGERED_KEY_IN_SESSION_STORAGE="vaadin.live-reload.triggered";E.TRIGGERED_COUNT_KEY_IN_SESSION_STORAGE="vaadin.live-reload.triggeredCount";E.AUTO_DEMOTE_NOTIFICATION_DELAY=5e3;E.HOTSWAP_AGENT="HOTSWAP_AGENT";E.JREBEL="JREBEL";E.SPRING_BOOT_DEVTOOLS="SPRING_BOOT_DEVTOOLS";E.BACKEND_DISPLAY_NAME={HOTSWAP_AGENT:"HotswapAgent",JREBEL:"JRebel",SPRING_BOOT_DEVTOOLS:"Spring Boot Devtools"};g([w({type:String})],E.prototype,"url",void 0);g([w({type:Boolean,attribute:!0})],E.prototype,"liveReloadDisabled",void 0);g([w({type:String})],E.prototype,"backend",void 0);g([w({type:Number})],E.prototype,"springBootLiveReloadPort",void 0);g([w({type:Boolean,attribute:!1})],E.prototype,"expanded",void 0);g([w({type:Array,attribute:!1})],E.prototype,"messages",void 0);g([w({type:String,attribute:!1})],E.prototype,"splashMessage",void 0);g([w({type:Array,attribute:!1})],E.prototype,"notifications",void 0);g([w({type:String,attribute:!1})],E.prototype,"frontendStatus",void 0);g([w({type:String,attribute:!1})],E.prototype,"javaStatus",void 0);g([I()],E.prototype,"tabs",void 0);g([I()],E.prototype,"activeTab",void 0);g([I()],E.prototype,"features",void 0);g([I()],E.prototype,"unreadErrors",void 0);g([st(".window")],E.prototype,"root",void 0);g([st("vaadin-dev-tools-component-picker")],E.prototype,"componentPicker",void 0);g([I()],E.prototype,"componentPickActive",void 0);g([I()],E.prototype,"themeEditorState",void 0);E=z=g([V("vaadin-dev-tools")],E);const{toString:Vs}=Object.prototype;function Ds(t){return Vs.call(t)==="[object RegExp]"}function Us(t,{preserve:e=!0,whitespace:o=!0,all:i}={}){if(i)throw new Error("The `all` option is no longer supported. Use the `preserve` option instead.");let a=e,n;typeof e=="function"?(a=!1,n=e):Ds(e)&&(a=!1,n=d=>e.test(d));let r=!1,l="",s="",c="";for(let d=0;d<t.length;d++){if(l=t[d],t[d-1]!=="\\"&&(l==='"'||l==="'")&&(r===l?r=!1:r||(r=l)),!r&&l==="/"&&t[d+1]==="*"){const h=t[d+2]==="!";let m=d+2;for(;m<t.length;m++){if(t[m]==="*"&&t[m+1]==="/"){a&&h||n&&n(s)?c+=`/*${s}*/`:o||(t[m+2]===`
`?m++:t[m+2]+t[m+3]===`\r
`&&(m+=2)),s="";break}s+=t[m]}d=m+1;continue}c+=l}return c}const qs=CSSStyleSheet.toString().includes("document.createElement"),Fs=(t,e)=>{const o=/(?:@media\s(.+?))?(?:\s{)?\@import\s*(?:url\(\s*['"]?(.+?)['"]?\s*\)|(["'])((?:\\.|[^\\])*?)\3)([^;]*);(?:})?/g;/\/\*(.|[\r\n])*?\*\//gm.exec(t)!=null&&(t=Us(t));for(var i,a=t;(i=o.exec(t))!==null;){a=a.replace(i[0],"");const n=document.createElement("link");n.rel="stylesheet",n.href=i[2]||i[4];const r=i[1]||i[5];r&&(n.media=r),e===document?document.head.appendChild(n):e.appendChild(n)}return a},Bs=(t,e,o)=>(o?e.adoptedStyleSheets=[t,...e.adoptedStyleSheets]:e.adoptedStyleSheets=[...e.adoptedStyleSheets,t],()=>{e.adoptedStyleSheets=e.adoptedStyleSheets.filter(i=>i!==t)}),Hs=(t,e,o)=>{const i=new CSSStyleSheet;return i.replaceSync(t),qs?Bs(i,e,o):(o?e.adoptedStyleSheets.splice(0,0,i):e.adoptedStyleSheets.push(i),()=>{e.adoptedStyleSheets.splice(e.adoptedStyleSheets.indexOf(i),1)})},Ws=(t,e)=>{const o=document.createElement("style");o.type="text/css",o.textContent=t;let i;if(e){const n=Array.from(document.head.childNodes).filter(r=>r.nodeType===Node.COMMENT_NODE).find(r=>r.data.trim()===e);n&&(i=n)}return document.head.insertBefore(o,i),()=>{o.remove()}},Ke=(t,e,o,i)=>{if(o===document){const n=Gs(t);if(window.Vaadin.theme.injectedGlobalCss.indexOf(n)!==-1)return;window.Vaadin.theme.injectedGlobalCss.push(n)}const a=Fs(t,o);return o===document?Ws(a,e):Hs(a,o,i)};window.Vaadin=window.Vaadin||{};window.Vaadin.theme=window.Vaadin.theme||{};window.Vaadin.theme.injectedGlobalCss=[];function ta(t){let e,o,i=2166136261;for(e=0,o=t.length;e<o;e++)i^=t.charCodeAt(e),i+=(i<<1)+(i<<4)+(i<<7)+(i<<8)+(i<<24);return("0000000"+(i>>>0).toString(16)).substr(-8)}function Gs(t){let e=ta(t);return e+ta(e+t)}/**
 * @license
 * Copyright (c) 2017 - 2023 Vaadin Ltd.
 * This program is available under Apache License Version 2.0, available at https://vaadin.com/license/
 */const Ks=t=>class extends t{static get properties(){return{_theme:{type:String,readOnly:!0}}}static get observedAttributes(){return[...super.observedAttributes,"theme"]}attributeChangedCallback(o,i,a){super.attributeChangedCallback(o,i,a),o==="theme"&&this._set_theme(a)}};/**
 * @license
 * Copyright (c) 2017 - 2023 Vaadin Ltd.
 * This program is available under Apache License Version 2.0, available at https://vaadin.com/license/
 */const Ra=[];function Na(t){return t&&Object.prototype.hasOwnProperty.call(t,"__themes")}function Ys(t){return Na(customElements.get(t))}function Js(t=[]){return[t].flat(1/0).filter(e=>e instanceof Mo?!0:(console.warn("An item in styles is not of type CSSResult. Use `unsafeCSS` or `css`."),!1))}function T(t,e,o={}){t&&Ys(t)&&console.warn(`The custom element definition for "${t}"
      was finalized before a style module was registered.
      Make sure to add component specific style modules before
      importing the corresponding custom element.`),e=Js(e),window.Vaadin&&window.Vaadin.styleModules?window.Vaadin.styleModules.registerStyles(t,e,o):Ra.push({themeFor:t,styles:e,include:o.include,moduleId:o.moduleId})}function Io(){return window.Vaadin&&window.Vaadin.styleModules?window.Vaadin.styleModules.getAllThemes():Ra}function Xs(t,e){return(t||"").split(" ").some(o=>new RegExp(`^${o.split("*").join(".*")}$`,"u").test(e))}function Qs(t=""){let e=0;return t.startsWith("lumo-")||t.startsWith("material-")?e=1:t.startsWith("vaadin-")&&(e=2),e}function Ia(t){const e=[];return t.include&&[].concat(t.include).forEach(o=>{const i=Io().find(a=>a.moduleId===o);i?e.push(...Ia(i),...i.styles):console.warn(`Included moduleId ${o} not found in style registry`)},t.styles),e}function Zs(t,e){const o=document.createElement("style");o.innerHTML=t.map(i=>i.cssText).join(`
`),e.content.appendChild(o)}function el(t){const e=`${t}-default-theme`,o=Io().filter(i=>i.moduleId!==e&&Xs(i.themeFor,t)).map(i=>({...i,styles:[...Ia(i),...i.styles],includePriority:Qs(i.moduleId)})).sort((i,a)=>a.includePriority-i.includePriority);return o.length>0?o:Io().filter(i=>i.moduleId===e)}const Dl=t=>class extends Ks(t){static finalize(){if(super.finalize(),this.elementStyles)return;const o=this.prototype._template;!o||Na(this)||Zs(this.getStylesForThis(),o)}static finalizeStyles(o){const i=this.getStylesForThis();return o?[...super.finalizeStyles(o),...i]:i}static getStylesForThis(){const o=Object.getPrototypeOf(this.prototype),i=(o?o.constructor.__themes:[])||[];this.__themes=[...i,...el(this.is)];const a=this.__themes.flatMap(n=>n.styles);return a.filter((n,r)=>r===a.lastIndexOf(n))}};/**
 * @license
 * Copyright (c) 2017 - 2023 Vaadin Ltd.
 * This program is available under Apache License Version 2.0, available at https://vaadin.com/license/
 */const tl=(t,...e)=>{const o=document.createElement("style");o.id=t,o.textContent=e.map(i=>i.toString()).join(`
`).replace(":host","html"),document.head.insertAdjacentElement("afterbegin",o)},ol=`.measurement-template-list-component .measurement-template-list{height:fit-content}.measurement-template-list-component .header{display:flex;justify-content:space-between;align-items:center;gap:var(--lumo-space-m)}.measurement-template-list-component .measurement-template-list-item{display:inline-flex;align-items:flex-start;justify-content:space-between;width:100%;column-gap:var(--lumo-space-l);padding-top:var(--lumo-space-s);padding-bottom:var(--lumo-space-s)}.measurement-template-list-component .measurement-template-list-item .controls{display:inline-flex;align-items:end;justify-items:center;column-gap:var(--lumo-space-s);padding-inline:var(--lumo-space-m)}.measurement-template-list-component .measurement-template-list-item .file-name{overflow:hidden;text-overflow:ellipsis}.measurement-template-list-component .measurement-template-list-item .file-icon{display:inline-flex;font-size:smaller;flex-shrink:0;color:var(--lumo-tertiary-text-color)}.measurement-template-list-component .measurement-template-list-item .file-info{display:inline-flex;flex-direction:column;gap:var(--lumo-space-s);overflow:hidden}.measurement-template-list-component .measurement-template-list-item .file-info-with-icon{display:inline-flex;gap:var(--lumo-space-s);overflow:hidden;align-items:center}.offer-list-component .header{display:inline-flex;justify-content:space-between;align-items:baseline}.offer-list-component .offer-info{display:inline-flex;align-items:center;justify-content:space-between;width:100%;column-gap:var(--lumo-space-l);padding-top:var(--lumo-space-s);padding-bottom:var(--lumo-space-s)}.offer-list-component .offer-info .controls{display:inline-flex;align-items:end;justify-items:center;column-gap:var(--lumo-space-s);padding-inline:var(--lumo-space-m)}.offer-list-component .offer-info .file-icon{display:inline-flex;font-size:smaller;flex-shrink:0;color:var(--lumo-tertiary-text-color)}.offer-list-component .offer-info .file-info{display:inline-flex;gap:var(--lumo-space-s);align-items:center;overflow:hidden}.offer-list-component .offer-info .file-name{overflow:hidden;text-overflow:ellipsis}.offer-list-component .offer-list{height:fit-content}.offer-list-component .offer-list .header{display:flex;justify-content:space-between;align-items:center;gap:var(--lumo-space-m)}.offer-list-component .offer-list .offer-list-item{display:inline-flex;justify-content:space-between}.offer-list-component .offer-info .signed-info{width:var(--lumo-icon-size-s);flex-shrink:0;justify-self:right}.offer-list-component .offer-info .signed-info.signed{color:var(--lumo-primary-color)}.offer-list-component .offer-info .signed-info.unsigned{color:var(--lumo-tertiary-text-color)}.quality-control-list-component .quality-control-list{height:fit-content}.quality-control-list-component .header{display:flex;justify-content:space-between;align-items:center;gap:var(--lumo-space-m)}.quality-control-list-component .quality-control-item{display:inline-flex;align-items:flex-start;justify-content:space-between;width:100%;column-gap:var(--lumo-space-l);padding-top:var(--lumo-space-s);padding-bottom:var(--lumo-space-s)}.quality-control-list-component .quality-control-item .controls{display:inline-flex;align-items:end;justify-items:center;column-gap:var(--lumo-space-s);padding-inline:var(--lumo-space-m)}.quality-control-list-component .quality-control-item .file-name{overflow:hidden;text-overflow:ellipsis}.quality-control-list-component .quality-control-item .file-icon{display:inline-flex;font-size:smaller;flex-shrink:0;color:var(--lumo-tertiary-text-color)}.quality-control-list-component .quality-control-item .file-info{display:inline-flex;flex-direction:column;gap:var(--lumo-space-s);overflow:hidden}.quality-control-list-component .quality-control-item .file-info-with-icon{display:inline-flex;gap:var(--lumo-space-s);overflow:hidden}
`,il=`h2{margin-bottom:.5em;margin-top:1.25em}vaadin-menu-bar-button{cursor:pointer}vaadin-menu-bar-item{cursor:pointer}vaadin-form-item::part(content){cursor:pointer}vaadin-multi-select-combo-box.chip-badge vaadin-multi-select-combo-box-chip{color:var(--lumo-primary-text-color);background-color:var(--lumo-primary-color-10pct);font-size:var(--lumo-font-size-s)}vaadin-multi-select-combo-box-item{align-items:start}vaadin-multi-select-combo-box.chip-badge vaadin-multi-select-combo-box-chip[slot=overflow]:before,vaadin-multi-select-combo-box-chip[slot=overflow]:after{border-color:var(--lumo-primary-color-10pct)}vaadin-multi-select-combo-box::part(toggle-button):before{color:var(--lumo-primary-color)}vaadin-multi-select-combo-box.no-chevron::part(toggle-button){display:none}vaadin-number-field::part(decrease-button),vaadin-number-field::part(increase-button){color:var(--lumo-primary-color)}vaadin-tabsheet.minimal::part(tabs-container){box-shadow:none;--_lumo-tab-marker-display: none}vaadin-list-box.transparent-icons vaadin-item::part(checkmark):before{display:none}vaadin-menu-bar-item.transparent-icon::part(checkmark):before{color:transparent}
`,al=`.spreadsheet,.spreadsheet-container{width:100%;height:100%}
`,nl=`.code-block{display:inline-flex;border-radius:var(--lumo-border-radius-m);align-items:center;padding-inline:var(--lumo-space-s);justify-content:space-between;border:1px solid var(--lumo-contrast-20pct);column-gap:var(--lumo-space-l);font-family:Source Code Pro for Powerline,monospace}.experiment-list-item{display:flex;gap:var(--lumo-space-s);font-size:var(--lumo-font-size-l)}.experiment-list-item vaadin-icon{font-size:var(--lumo-font-size-m);padding:var(--lumo-space-xs)}.error-text{color:var(--lumo-error-text-color)}.info-box{display:inline-flex;border-radius:var(--lumo-border-radius-m);column-gap:var(--lumo-space-s);background-color:var(--lumo-contrast-5pct);align-items:center;width:fit-content;padding-inline:var(--lumo-space-m);padding-top:var(--lumo-space-s);padding-bottom:var(--lumo-space-s);font-size:small}.ontology-link{display:inline-flex;box-sizing:border-box;padding:.4em calc(.5em + var(--lumo-border-radius-s) / 4);color:var(--lumo-primary-text-color);background-color:var(--lumo-primary-color-10pct);border-radius:var(--lumo-border-radius-s);font-family:var(--lumo-font-family);font-size:var(--lumo-font-size-s);line-height:1;text-decoration-line:underline;width:min-content}span.bold{font-weight:700}span.clickable{cursor:pointer}span.primary{color:var(--lumo-primary-text-color)}span.secondary{color:var(--lumo-secondary-text-color);font-size:var(--lumo-font-size-s)}span.tertiary{color:var(--lumo-tertiary-text-color);font-size:var(--lumo-font-size-s)}span.warning{color:var(--lumo-warning-text-color);font-size:var(--lumo-font-size-s)}span.inline{display:inline-flex;column-gap:var(--lumo-space-xs)}.spreadsheet-list-item{display:inline-flex;width:100%;justify-content:space-between;align-items:center}.tag{display:inline-flex;align-items:center;justify-content:center;box-sizing:border-box;padding:.4em calc(.5em + var(--lumo-border-radius-s) / 4);border-radius:var(--lumo-border-radius-s);font-family:var(--lumo-font-family);font-size:var(--lumo-font-size-s);line-height:1;font-weight:500;text-transform:initial;letter-spacing:initial;min-width:calc(var(--lumo-line-height-xs) * 1em + .45em);flex-shrink:0}.tag:before{display:inline-block;content:" ";width:0}.tag.contrast{background-color:var(--lumo-contrast-5pct);color:var(--lumo-contrast-80pct)}.tag.error{background-color:var(--lumo-error-color-10pct);color:var(--lumo-error-color)}.tag.primary{background-color:var(--lumo-primary-color-10pct);color:var(--lumo-primary-text-color)}.tag.success{background-color:var(--lumo-success-color-10pct);color:var(--lumo-success-text-color)}.tag.violet{background-color:#7b61ff1a;color:#7b61ff}.tag.pink{background-color:#ff5dd226;color:#df0b92}.tag.warning{background-color:var(--lumo-warning-color-10pct);color:var(--lumo-warning-text-color)}
`,rl=`.page{height:100%;width:100%}.page-area{background-color:var(--lumo-base-color);padding:var(--lumo-space-m);flex-direction:column;display:flex;gap:var(--lumo-space-s)}.page-area .title{font-weight:700;color:var(--lumo-secondary-text-color);font-size:var(--lumo-font-size-xxl);margin-bottom:.5rem}.page-area.navbar{justify-content:space-evenly;display:flex;flex-direction:row;align-items:center;gap:var(--lumo-space-xs)}.batch-details-component .editor-buttons{display:inline-flex;gap:var(--lumo-space-s)}.batch-details-component .title-and-controls{display:inline-flex;justify-content:space-between;align-items:baseline}.experiment-details-component .sample-source-display{flex-direction:row;display:flex;justify-content:space-between}.experiment-details-component .sample-source-display .sample-source{flex-direction:column;display:flex;row-gap:var(--lumo-space-m)}.experiment-details-component .sample-source-display .sample-source .header{display:inline-flex;justify-content:start;align-items:start;column-gap:var(--lumo-space-m);font-size:var(--lumo-font-size-m);font-weight:500}.experiment-details-component .sample-source-display .sample-source .ontologies{display:flex;row-gap:var(--lumo-space-s);align-items:center;width:100%;flex-direction:column}.experiment-details-component .sample-source-display .sample-source .ontologies .ontology{display:inline-flex;gap:var(--lumo-space-s);align-items:center;width:100%}.experiment-details-component .sample-source-display .icon-with-list vaadin-icon{color:var(--lumo-primary-color);margin-right:1em;margin-top:.5em}.experiment-details-component .header{display:flex;justify-content:space-between;align-items:baseline}.experiment-details-component .details-content{padding:var(--lumo-space-m);display:flex;flex-direction:column;flex-grow:1;gap:var(--lumo-space-m);font-size:var(--lumo-font-size-s)}.experiment-details-component vaadin-tabsheet{height:100%;width:100%}.experiment-details-component .details-content .experimental-groups-container,.experiment-details-component .details-content .experimental-variables-container{height:100%;width:100%}.experiment-details-component .sample-registration-possible{margin-bottom:var(--lumo-space-m)}.experiment-list-component .content{display:grid;gap:var(--lumo-space-m);grid-auto-rows:max-content}.experiment-list-component .header{display:flex;justify-content:space-between;align-items:center;gap:var(--lumo-space-m)}.measurement-details-component{height:100%;justify-content:center;align-items:center}.measurement-details-component .measurement-tabsheet{height:100%;width:100%}.measurement-details-component .measurement-grid{height:100%}.measurement-details-component .measurement-grid .measurement-column-cell,.measurement-details-component .measurement-grid .sample-column-cell{display:inline-flex;column-gap:var(--lumo-space-s);align-items:center}.measurement-details-component .measurement-grid .sample-column-cell .expand-icon{width:1em;height:1em;color:var(--lumo-primary-color);cursor:pointer}.measurement-details-component .measurement-grid .organisation-entry{align-items:center;column-gap:var(--lumo-space-s);display:flex}.measurement-details-component .measurement-grid .organisation-entry .organisation-icon{min-width:var(--lumo-icon-size-m);min-height:var(--lumo-icon-size-m)}.measurement-details-component .measurement-grid .instrument-column{column-gap:var(--lumo-space-s);align-items:center;display:inline-flex}.measurement-details-component .measurement-grid .measurement-item{display:flex;flex-direction:column;row-gap:var(--lumo-space-s);cursor:default}.measurement-details-component .measurement-grid .measurement-item .entry{display:inline-flex;column-gap:var(--lumo-space-s)}.measurement-details-component .measurement-grid .measurement-item .entry .entry-label{font-weight:700;white-space:nowrap}.ontology-lookup-component{display:flex;flex-direction:column;row-gap:var(--lumo-space-m)}.ontology-lookup-component .search-field{max-width:33%}.ontology-lookup-component .ontology-grid-section{border-top:1px solid var(--lumo-contrast-10pct);margin-right:var(--lumo-space-xl);row-gap:var(--lumo-space-s);padding-top:var(--lumo-space-m);height:100%;display:flex;flex-direction:column}.ontology-lookup-component .ontology-grid-section .ontology-grid{height:100%;margin-top:var(--lumo-space-s)}.ontology-lookup-component .ontology-grid vaadin-grid-cell-content{padding:var(--lumo-space-xs)}.ontology-lookup-component .ontology-item{border:black;border-radius:var(--lumo-border-radius-m);box-shadow:var(--lumo-box-shadow-s);box-sizing:border-box;display:flex;flex-direction:column;flex-wrap:wrap;margin-bottom:var(--lumo-space-s);margin-top:var(--lumo-space-s);overflow:hidden;padding:var(--lumo-space-m);row-gap:var(--lumo-space-s);text-overflow:ellipsis;white-space:normal}.ontology-lookup-component .ontology-item .header{display:inline-flex;column-gap:var(--lumo-space-s);align-items:center}.ontology-lookup-component .ontology-item .header .copy-icon{display:inline-flex;font-size:smaller;flex-shrink:0;color:var(--lumo-tertiary-text-color)}.copy-icon-success{display:inline-flex;font-size:smaller;flex-shrink:0;color:var(--lumo-success-color)}.success-background-hue{background-color:var(--lumo-success-color-10pct)}.base-background{background-color:var(--lumo-base-color)}.ontology-lookup-component .ontology-item .ontology-item-title{font-weight:700;color:var(--lumo-secondary-text-color);font-size:var(--lumo-font-size-l)}.ontology-lookup-component .ontology-item .url{display:inline-flex;font-size:smaller;flex-shrink:0;color:var(--lumo-tertiary-text-color)}.personal-access-token-component{row-gap:var(--lumo-space-l);display:flex}.personal-access-token-component .header{justify-content:space-between;display:inline-flex}.personal-access-token-component .header .buttons{column-gap:var(--lumo-space-s);display:inline-flex}.personal-access-token-component .description{display:flex;flex-direction:column;row-gap:var(--lumo-space-m)}.personal-access-token-component .personal-access-token-container{width:60%;height:60%}.personal-access-token-component .personal-access-token-container .personal-access-token-list{border:1px solid var(--lumo-contrast-20pct)}.personal-access-token-component .personal-access-token-container .show-created-personal-access-token-details{display:flex;flex-direction:column;row-gap:var(--lumo-space-s);padding:var(--lumo-space-m);border:1px solid var(--lumo-contrast-20pct);width:100%}.personal-access-token-component .personal-access-token-container .show-created-personal-access-token-layout{display:flex;justify-content:space-between;align-items:center}.personal-access-token-component .personal-access-token-container .show-created-personal-access-token-details .copy-disclaimer{display:inline-flex;column-gap:var(--lumo-space-s);font-size:var(--lumo-font-size-s);align-items:center}.personal-access-token-component .personal-access-token-container .show-created-personal-access-token-details .token-text{display:inline-flex;column-gap:var(--lumo-space-s);align-items:center}.personal-access-token-component .personal-access-token-container .show-encrypted-personal-access-token-details{display:flex;flex-direction:column;row-gap:var(--lumo-space-m)}.personal-access-token-component .personal-access-token-container .show-encrypted-personal-access-token-details .copy-disclaimer,.personal-access-token-component .personal-access-token-container .show-encrypted-personal-access-token-details .expiration-date,.personal-access-token-component .personal-access-token-container .show-encrypted-personal-access-token-details .token-text{display:inline-flex;column-gap:var(--lumo-space-s);align-items:center}.personal-access-token-component .personal-access-token-container .show-encrypted-personal-access-token-layout{display:flex;padding:var(--lumo-space-m);border-bottom:1px solid var(--lumo-contrast-20pct);justify-content:space-between;align-items:center}.project-access-component{display:flex;row-gap:var(--lumo-space-l);flex-direction:column;width:clamp(500px,70%,100%);height:max-content}.project-access-component .header{display:inline-flex;justify-content:space-between;align-items:baseline}.project-access-component .change-project-access-cell{display:inline-flex;column-gap:var(--lumo-space-l);align-items:center}.project-collection-component{height:100%;border-top:none}.project-collection-component .header{display:flex;column-gap:var(--lumo-space-xl);flex-direction:column}.project-collection-component .controls{display:flex;align-content:space-between;gap:1rem}.project-collection-component .project-grid{height:100%;margin-top:var(--lumo-space-s)}.project-collection-component .project-grid vaadin-grid-cell-content{padding:var(--lumo-space-xs)}.project-collection-component .project-overview-item{border:black;border-radius:var(--lumo-border-radius-m);box-shadow:var(--lumo-box-shadow-s);box-sizing:border-box;display:flex;flex-direction:column;flex-wrap:wrap;margin-bottom:var(--lumo-space-s);margin-top:var(--lumo-space-s);overflow:hidden;padding:var(--lumo-space-l);row-gap:var(--lumo-space-s);text-overflow:ellipsis;white-space:normal;cursor:pointer}.project-collection-component .project-overview-item .header{display:inline-flex;column-gap:var(--lumo-space-s);flex-direction:row}.project-collection-component .project-overview-item .project-overview-item-title{font-weight:700;color:var(--lumo-secondary-text-color);font-size:var(--lumo-font-size-l)}.project-collection-component .project-overview-item .details{display:flex;flex-direction:column;row-gap:var(--lumo-space-s);color:var(--lumo-secondary-text-color)}.project-details-component .header{display:flex;justify-content:space-between;align-items:baseline}.quality-control-list-component .header{display:inline-flex;justify-content:space-between;align-items:baseline}.raw-data-details-component .raw-data-details{display:flex;flex-direction:column;row-gap:var(--lumo-space-s)}.raw-data-details-component .raw-data-grid .sample-information{text-overflow:ellipsis;overflow:hidden;display:inline-flex;white-space:nowrap;width:80%}.raw-data-details-component .raw-data-grid>*{cursor:pointer}.raw-data-details-component .raw-data-grid ::part(row){cursor:pointer}.raw-data-details-component .raw-data-grid .raw-data-item{display:flex;flex-direction:column;row-gap:var(--lumo-space-s);cursor:default}.raw-data-details-component .raw-data-grid .raw-data-item .entry{display:inline-flex;column-gap:var(--lumo-space-s)}.raw-data-details-component .raw-data-grid .raw-data-item .entry .entry-label{font-weight:700;white-space:nowrap}.raw-data-details-component .raw-data-grid .raw-data-item .entry-value-list{display:flex;flex-wrap:wrap;gap:var(--lumo-space-s)}.raw-data-details-component .raw-data-grid .raw-data-item .entry-value{display:inline-flex;white-space:nowrap}.raw-data-download-information-component{display:flex;flex-direction:column;row-gap:var(--lumo-space-l)}.raw-data-download-information-component .section .section-title{display:inline-flex;align-items:center;column-gap:var(--lumo-space-m);font-weight:700}.project-details-component .ontology-entry-collection{display:inline-flex;flex-wrap:wrap;gap:var(--lumo-space-xs);width:100%;white-space:nowrap}.sample-details-component .button-and-search-bar{display:flex;justify-content:space-between;gap:var(--lumo-space-s);margin-bottom:var(--lumo-space-m)}.sample-details-component .button-bar{gap:var(--lumo-space-s);display:inline-flex;align-items:end}.sample-details-component .sample-details-content{display:flex;flex-direction:column;margin-bottom:var(--lumo-space-m);height:100%}.sample-details-component .sample-tab-content{height:100%;width:100%}.sample-details-component .search-bar{gap:var(--lumo-space-s);display:inline-flex;align-items:end}.user-profile-component{display:flex;flex-direction:column;row-gap:var(--lumo-space-xl)}.user-profile-component .user-details-card{display:flex;flex-direction:row;column-gap:5rem;background-color:var(--lumo-base-color);border:black;border-radius:var(--lumo-border-radius-m);box-shadow:var(--lumo-box-shadow-s);box-sizing:border-box;overflow:hidden;text-overflow:ellipsis;padding:5rem;min-width:fit-content;width:66%}.user-profile-component .change-name{color:var(--lumo-primary-text-color);cursor:pointer;width:fit-content}.user-profile-component .user-details-card .details{display:flex;flex-direction:column;row-gap:var(--lumo-space-l)}.user-profile-component .user-details-card .detail{display:flex;flex-direction:column;row-gap:var(--lumo-space-s)}.user-profile-component .user-details-card .avatar-with-name{display:flex;flex-direction:column;row-gap:var(--lumo-space-l);column-gap:initial}.user-profile-component .user-details-card .user-avatar{height:5rem;width:5rem}
`,sl=`.project-navigation-drawer{display:flex;flex-direction:column;gap:var(--lumo-space-m);margin-inline:var(--lumo-space-s)}.project-navigation-drawer-title{font-weight:700;font-size:var(--lumo-font-size-xl);margin-left:var(--lumo-space-s);margin-top:var(--lumo-space-s);margin-bottom:var(--lumo-space-l)}.project-navigation-drawer vaadin-side-nav-item.primary::part(item),.project-navigation-drawer vaadin-side-nav-item.primary [slot=prefix],.project-navigation-drawer vaadin-side-nav-item.primary [slot=suffix],.project-navigation-drawer vaadin-side-nav-item::part(toggle-button){color:var(--lumo-primary-color)}.project-navigation-drawer vaadin-side-nav-item.hoverable::part(item):hover{background-color:var(--lumo-primary-color-10pct)}.project-navigation-drawer .content{display:inline-flex;flex-direction:column;gap:var(--lumo-space-l)}.project-navigation-drawer .project-section{display:flex;flex-direction:column}.project-navigation-drawer .project-items{display:flex;flex-direction:column;gap:var(--lumo-space-m)}.project-navigation-drawer .section-divider{margin-top:var(--lumo-space-m);margin-bottom:var(--lumo-space-m)}.project-selection-menu{margin-left:var(--lumo-space-s);margin-right:var(--lumo-space-s)}.project-selection-menu vaadin-menu-bar-button[aria-haspopup]{overflow:hidden;text-overflow:ellipsis;width:98%}.project-selection-menu vaadin-menu-bar-item::part(content){width:100%}.recent-projects-header{color:var(--lumo-tertiary-text-color);margin-left:var(--lumo-space-l)}.dropdown-field{display:inline-flex;justify-content:space-between;width:100%}.selected-project-title{text-overflow:ellipsis;overflow:hidden}
`,ll=`.data-manager-layout::part(navbar){display:inline-flex;justify-content:space-between;background-image:linear-gradient(var(--lumo-contrast-5pct),var(--lumo-contrast-5pct))}.data-manager-layout .data-manager-title{font-size:var(--lumo-font-size-l);color:var(--lumo-header-text-color);font-weight:600;line-height:var(--lumo-line-height-xs);margin-block:0}.data-manager-menu{display:inline-flex;justify-content:space-between;column-gap:var(--lumo-space-s)}.data-manager-menu .menubar::part(container){display:inline-flex;column-gap:var(--lumo-space-s)}.data-manager-menu .user-avatar{cursor:pointer}.experiment-main-layout .experiment-app-navbar{display:inline-flex;justify-content:space-between;padding-right:var(--lumo-space-m)}.drawer-title-bar{display:inline-flex;align-items:center}.experiment-navigation-component{width:100%;padding-top:var(--lumo-space-s);padding-bottom:var(--lumo-space-s);background-color:var(--lumo-base-color)}.experiment-navigation-component .experiment-navigation-tabs{width:100%}.experiment-navigation-component .experiment-navigation-tabs .arrow-tab{align-items:end;color:var(--lumo-contrast-60pct)}.project-main-layout .project-main-layout-navbar{display:inline-flex;justify-content:space-between;width:100%;padding-right:var(--lumo-space-m);align-items:center}.experiment-main-layout .experiment-main-layout-navbar{padding-top:var(--lumo-space-s);padding-bottom:var(--lumo-space-s);display:inline-flex;justify-content:space-between;padding-right:var(--lumo-space-m);align-items:center}.project-navbar-title{font-weight:700;font-size:var(--lumo-font-size-l);display:inline-flex;align-items:inherit;column-gap:var(--lumo-space-s)}.experiment-main-layout .experiment-main-layout-navbar-container{display:flex;flex-direction:column;width:100%}#landing-page-layout::part(navbar){padding-inline:var(--lumo-space-m)}.user-main-layout::part(navbar){padding-inline:var(--lumo-space-m)}.navbar-title{font-weight:700;display:inline-flex;align-items:inherit;column-gap:var(--lumo-space-s)}
`,cl=`#content-area{grid-area:content-area}#main-layout{display:grid;grid-template-columns:1fr;grid-template-rows:minmax(max-content,95%) minmax(min-content,5%);height:100%;grid-template-areas:"content-area" "data-manager-footer"}#landing-page-layout .landing-page-content{background-size:cover;background-position:center;background-repeat:no-repeat;height:100%}#landing-page-layout .landing-page-title-and-logo{display:flex;align-items:center;justify-content:center;flex-direction:column;row-gap:var(--lumo-space-s);margin-bottom:var(--lumo-space-l);padding-top:var(--lumo-space-xl)}#landing-page-layout .landing-page-title-and-logo .title{font-weight:700;font-size:var(--lumo-font-size-xxl)}#landing-page-layout .landing-page-title-and-logo .subtitle{color:var(--lumo-tertiary-text-color);font-weight:700}#landing-page-layout .landing-page-title-and-logo .ut-logo{height:2.5em;margin-bottom:var(--lumo-space-xl)}#data-manager-footer{display:inline-flex;column-gap:var(--lumo-space-l);border-top:thin solid var(--lumo-contrast-10pct);padding-inline:var(--lumo-space-m);padding-bottom:var(--lumo-space-s);padding-top:var(--lumo-space-s);grid-area:data-manager-footer}.main{display:grid;height:100%}.main .title{font-weight:700;color:var(--lumo-secondary-text-color);font-size:var(--lumo-font-size-xxl);margin-bottom:.5rem}.main.experiment{grid-template-columns:minmax(max-content,100%);grid-template-rows:auto;grid-template-areas:"experimentdetails"}.main.experiment .experiment-details-component{grid-area:experimentdetails}.main.legal-notice{padding:var(--lumo-space-xl);height:fit-content;width:clamp(700px,60vw,100%);display:flex;margin:auto}.main.data-privacy-agreement{padding:var(--lumo-space-xl);height:fit-content;width:clamp(700px,50vw,100%);display:flex;margin:auto}.main.measurement{grid-template-columns:minmax(max-content,70%) minmax(max-content,30%);grid-template-rows:minmax(max-content,25%) minmax(max-content,70%);grid-template-areas:". measurementtemplatelist" "measurementdetails measurementdetails"}.main.measurement .page-area{border:1px solid;border-color:var(--lumo-contrast-10pct);border-radius:var(--lumo-border-radius-m)}.main.measurement .no-samples-registered-disclaimer,.main.measurement .no-measurements-registered-disclaimer{grid-column-start:1;grid-column-end:-1;grid-row-start:1;grid-row-end:-1}.main.measurement .no-measurements-registered-disclaimer{display:flex;flex-direction:column;row-gap:var(--lumo-space-m);align-items:center;justify-content:center}.main.measurement .no-measurements-registered-disclaimer .no-measurement-registered-title{font-weight:700;font-size:var(--lumo-font-size-m);margin-bottom:.5rem}.main.measurement .no-measurements-registered-disclaimer .no-measurement-registered-content{display:flex;flex-direction:column;align-items:center}.main.measurement .measurement-template-list-component{grid-area:measurementtemplatelist}.main.measurement .measurement-details-component{grid-area:measurementdetails}.main.measurement .measurement-main-content{display:flex;flex-direction:column;row-gap:var(--lumo-space-m);padding:var(--lumo-space-m)}.main.measurement .measurement-main-content .title{font-weight:700;color:var(--lumo-secondary-text-color);font-size:var(--lumo-font-size-xxl);margin-bottom:.5rem}.main.measurement .measurement-main-content .buttonsAndInfo{display:flex;flex-direction:column;row-gap:var(--lumo-space-m)}.main.measurement .measurement-main-content .buttonsAndInfo .info{border-radius:var(--lumo-border-radius-m);column-gap:var(--lumo-space-s);background-color:var(--lumo-contrast-5pct);align-items:center;width:fit-content;padding-inline:var(--lumo-space-m);padding-top:var(--lumo-space-s);padding-bottom:var(--lumo-space-s)}.main.measurement .measurement-main-content .buttonsAndInfo .buttonAndField{display:inline-flex;justify-content:space-between;margin-top:auto}.main.measurement .measurement-main-content .button-bar{display:inline-flex;column-gap:var(--lumo-space-s)}.main.ontology-lookup-main{grid-template-columns:auto;grid-template-rows:auto;grid-template-areas:"ontologylookupcomponent"}.main.ontology-lookup-component{grid-area:ontologylookupcomponent}.main.project{grid-template-columns:minmax(min-content,80%) minmax(min-content,25%);grid-template-rows:auto;grid-template-areas:"projectdetails experimentlist" "projectdetails offerlist" "projectdetails qualitycontrollist"}.main.project .page-area{border:1px solid;border-color:var(--lumo-contrast-10pct);border-radius:var(--lumo-border-radius-m)}.main.project .project-details-component{grid-area:projectdetails}.main.project .experiment-list-component{grid-area:experimentlist}.main.project .offer-list-component{grid-area:offerlist}.main.project .quality-control-list-component{grid-area:qualitycontrollist}.main.project-overview{grid-template-columns:minmax(min-content,1fr);grid-template-rows:minmax(min-content,10%) minmax(min-content,80%);grid-template-areas:"." "projectcollection"}.main.project-overview .title-and-description{padding:var(--lumo-space-m);flex-direction:column;display:flex;gap:var(--lumo-space-s);white-space:pre-line}.main.project-overview .project-overview-title{font-weight:700;color:var(--lumo-secondary-text-color);font-size:var(--lumo-font-size-xxl);margin-bottom:.5rem}.main .project-overview .project-collection-component{grid-area:projectcollection}.main.raw-data{grid-template-columns:minmax(min-content,80%) minmax(min-content,20%);grid-template-rows:minmax(min-content,20%) minmax(min-content,80%);grid-template-areas:". rawdatadownloadinformation" "rawdatadetails rawdatadownloadinformation"}.main.raw-data .page-area{border:1px solid;border-color:var(--lumo-contrast-10pct);border-radius:var(--lumo-border-radius-m)}.main.raw-data .no-measurements-registered-disclaimer,.main.raw-data .no-raw-data-registered-disclaimer{grid-column-start:1;grid-column-end:-1;grid-row-start:1;grid-row-end:-1}.main.raw-data .raw-data-download-information-component{grid-area:rawdatadownloadinformation}.main.raw-data .raw-data-details-component{grid-area:rawdatadetails}.main.raw-data .raw-data-main-content{display:flex;flex-direction:column;row-gap:var(--lumo-space-m);padding:var(--lumo-space-m)}.main.raw-data .raw-data-main-content .title{font-weight:700;color:var(--lumo-secondary-text-color);font-size:var(--lumo-font-size-xxl);margin-bottom:.5rem}.main.raw-data .raw-data-main-content .buttonAndField{display:inline-flex;justify-content:space-between;margin-top:auto}.main.sample{grid-template-columns:minmax(min-content,60%) minmax(min-content,50%);grid-template-rows:minmax(min-content,20%) minmax(min-content,75%);grid-template-areas:". batchdetails" "sampledetails sampledetails"}.main.sample .page-area{border:1px solid;border-color:var(--lumo-contrast-10pct);border-radius:var(--lumo-border-radius-m)}.main.sample .batch-details-component{grid-area:batchdetails}.main.sample .sample-details-component{grid-area:sampledetails}.main.user-profile{grid-template-columns:minmax(min-content,auto);grid-template-rows:minmax(min-content,auto);grid-template-areas:"user-profile-component"}@media only screen and (max-width: 1200px){.main.experiment{grid-template-columns:minmax(min-content,1fr);grid-template-areas:"experimentdetails";grid-template-rows:minmax(min-content,1fr)}.main.measurement{grid-template-columns:minmax(min-content,1fr);grid-template-areas:"." "measurementtemplatelist" "measurementdetails";grid-template-rows:minmax(min-content,20%) minmax(min-content,20%) minmax(min-content,60%)}.main.project{grid-template-columns:minmax(min-content,1fr);grid-template-areas:"projectdetails" "experimentlist" "offerlist" "qualitycontrollist";grid-template-rows:minmax(min-content,60%) minmax(min-content,20%) minmax(min-content,20%)}.main.project-overview{grid-template-columns:minmax(min-content,1fr);grid-template-rows:minmax(min-content,10%) minmax(min-content,80%);grid-template-areas:"." "projectcollection"}.main.raw-data{grid-template-columns:minmax(min-content,1fr);grid-template-areas:"." "rawdatadownloadinformation" "rawdatadetails";grid-template-rows:minmax(min-content,20%) minmax(min-content,20%) minmax(min-content,60%)}.main.sample{grid-template-columns:minmax(min-content,1fr);grid-template-areas:"batchdetails" "sampledetails";grid-template-rows:minmax(min-content,20%) minmax(min-content,75%)}}
`,dl=`.info-section{width:100%;display:grid;gap:1rem}.info-section>.info-title{font-weight:700;color:var(--lumo-secondary-text-color);font-size:var(--lumo-font-size-xl)}.info-section .info-entry-label{font-weight:700;font-size:var(--lumo-font-size-s);color:var(--lumo-secondary-text-color);display:flex;gap:var(--lumo-space-s)}.info-entry{display:grid;grid:auto / 20% auto}.info-content{display:grid;gap:.75rem}.project-information-content{display:grid;gap:4rem}.contact-item,.person-contact-display{display:grid}.contact-item .contact-email,.person-contact-display .email{font-size:var(--lumo-font-size-s);color:var(--lumo-secondary-text-color)}
`,ml=`img.clickable{cursor:pointer}
`,pl=`vaadin-icon.primary{color:var(--lumo-primary-color)}vaadin-icon.error{color:var(--lumo-error-color)}vaadin-icon.success{color:var(--lumo-success-color)}vaadin-icon.clickable{cursor:pointer}vaadin-icon.small{width:var(--lumo-icon-size-s)}vaadin-icon.smallest{width:1em}vaadin-icon.copy-icon{color:var(--lumo-contrast-50pct)}
`,ul=`.card-deck{display:flex;align-content:space-evenly;flex-flow:row wrap}.disclaimer{display:flex;justify-content:center;width:100%;height:100%}.disclaimer .disclaimer-content{display:flex;flex-direction:column;justify-content:center;align-items:center;gap:var(--lumo-space-s)}.disclaimer .disclaimer-label{font-weight:400;color:#2d2d2d;font-size:var(--lumo-font-size-s)}.disclaimer .disclaimer-title{font-weight:700;color:#2d2d2d;font-size:var(--lumo-font-size-m);margin-bottom:.5rem}.grid-details-item{display:flex;flex-direction:column;flex-wrap:wrap;gap:var(--lumo-space-s);cursor:default}.grid-details-item .entry{display:inline-flex;gap:var(--lumo-space-s)}.grid-details-item .entry .entry-label{font-weight:700;white-space:nowrap}.grid-details-item .entry .entry-value-list{display:flex;flex-wrap:wrap;gap:var(--lumo-space-s)}.grid-details-item .entry-value{display:inline-flex;flex-wrap:wrap;word-break:break-all}.ontology-component{display:flex;flex-direction:column;gap:var(--lumo-space-s)}.project-creation-stepper-arrow{align-items:end;color:var(--lumo-contrast-60pct);display:flex;margin-inline:var(--lumo-space-l)}.stepper{margin-top:.5rem;display:inline-flex;padding-inline:4rem}.stepper .step{display:flex;flex-direction:column;align-items:center;white-space:nowrap;gap:var(--lumo-space-s)}.stepper .step[selected]>vaadin-avatar{background-color:var(--lumo-primary-color-50pct);color:#fff}.stepper .step>vaadin-avatar{background-color:var(--lumo-contrast-60pct);color:#fff}.ontology-entry-collection{display:inline-flex;flex-wrap:wrap;gap:var(--lumo-space-s);width:100%;white-space:nowrap}.ontology-entry-collection .ontology-entry{display:inline-flex;gap:var(--lumo-space-s);width:100%}.project-role-item .project-role-label{font-weight:700}.project-role-item .project-role-description{display:flex;flex-direction:column}.tag-collection{display:inline-flex;flex-wrap:wrap;gap:var(--lumo-space-xs);white-space:nowrap}
`,hl=`.dialog-window .confirm-button:hover:before{opacity:.05}vaadin-dialog-overlay::part(content){padding:1rem 4rem 3rem}vaadin-dialog-overlay::part(footer){padding:1rem 4rem 1rem 0}vaadin-dialog-overlay::part(header){padding:2rem 4rem 1rem}vaadin-dialog-overlay::part(title){margin-inline-start:0}.notification-dialog::part(overlay){width:36.75rem}.notification-dialog .content{width:100%}.notification-dialog>[slot=header]{display:flex;align-items:center;justify-items:flex-start;gap:var(--lumo-space-s)}.notification-dialog .title{font-family:var(--lumo-font-family);line-height:var(--lumo-line-height-m);font-size:var(--lumo-font-size-xl);margin:0}.notification-dialog>[slot=header] vaadin-icon{width:1.83rem;height:1.83rem;margin:.46rem;margin-inline-start:calc(var(--lumo-space-l) - var(--lumo-space-m))}.notification-dialog .error-icon{fill:var(--lumo-error-color)}.notification-dialog .warning-icon{fill:var(--lumo-warning-color)}.notification-dialog .info-icon{fill:var(--lumo-primary-color)}.existing-groups-prevent-variable-edit .content .experimental-group-count,.existing-samples-prevent-variable-edit .content .sample-count,.existing-samples-prevent-group-edit .content .sample-count{font-weight:700}.add-personal-access-token-dialog::part(overlay){height:50%;min-width:50vw}.add-personal-access-token-dialog::part(content){display:flex;flex-direction:column}.add-personal-access-token-dialog .expiration-date{width:50%}.add-user-to-project-dialog::part(overlay){height:fit-content;min-width:fit-content}.add-user-to-project-dialog::part(content){display:flex;flex-direction:column;gap:var(--lumo-space-xl)}.add-user-to-project-dialog .person-selection-section,.add-user-to-project-dialog .role-selection-section{display:flex;flex-direction:column;gap:var(--lumo-space-s)}.add-user-to-project-dialog .section-title{color:var(--lumo-secondary-text-color);font-size:var(--lumo-font-size-l);font-weight:700}.batch-registration-dialog::part(content),.batch-update-dialog::part(content){display:flex;flex-direction:column;gap:var(--lumo-space-s)}.batch-update-dialog::part(overlay),.batch-registration-dialog::part(overlay){min-width:80vw;height:100%}.batch-update-dialog .batch-name,.batch-update-dialog .experiment-name,.batch-registration-dialog .experiment-name{margin:auto .3em;font-weight:bolder}.batch-registration-dialog .prefill-section{display:flex;justify-content:space-between;align-items:center}.batch-registration-dialog .spreadsheet-container,.batch-update-dialog .spreadsheet-container{display:flex;flex-direction:column;column-gap:var(--lumo-space-s);margin-top:var(--lumo-space-m)}.batch-registration-dialog .spreadsheet-header,.batch-update-dialog .spreadsheet-header{display:flex;justify-content:space-between;align-items:center}.batch-update-dialog .spreadsheet-controls,.batch-registration-dialog .spreadsheet-controls{display:inline-flex;column-gap:.5em;justify-content:end}.batch-update-dialog .spreadsheet-controls .error-text,.batch-registration-dialog .spreadsheet-controls .error-text{color:var(--lumo-error-text-color)}.batch-update-dialog .batch-controls>.batch-name-field,.batch-registration-dialog .batch-controls>.batch-name-field{width:calc(100% / 3)}.add-experiment-dialog::part(overlay),.edit-experiment-dialog::part(overlay){width:66vw}.edit-experiment-content,.add-experiment-content{padding-top:var(--lumo-space-m);display:flex;flex-direction:column;height:100%;width:100%;gap:var(--lumo-space-m);box-sizing:border-box}.edit-experiment-content .header,.add-experiment-content .header{font-weight:700}.edit-experiment-content .full-width-input,.add-experiment-content .full-width-input{width:100%}.input-with-icon-selection{display:inline-flex;column-gap:1rem}.icon-and-component{display:inline-flex;justify-content:start;align-items:flex-end;column-gap:var(--lumo-space-m)}.box-flexgrow{flex-grow:1}.edit-project-dialog .content{max-width:60vw;padding-bottom:1em}.edit-project-dialog::part(overlay){width:66vw}.edit-project-dialog .funding-field .input-fields,.contact-field .input-fields{display:flex;gap:1em;align-items:baseline}.contact-field .contact-selection,.contact-field .input-fields,.contact-field .input-fields>*{width:100%}.contact-field .prefill-input-fields{line-height:0;display:flex;flex-direction:column;align-items:baseline}.contact-field .prefill-input-fields .contact-self-select{width:36em}.edit-project-dialog .form-content .contact-field .name-field,.edit-project-dialog .form-content .contact-field .email-field{flex-grow:1}.edit-project-dialog .project-contacts{display:flex;gap:1em;flex-direction:column;flex-wrap:wrap;margin:0;padding:var(--lumo-space-m) 0 0}.experiment-group-dialog::part(overlay){width:66%}.experiment-group-dialog .number-field{min-width:175px}.experiment-group-dialog .text-field{min-width:100px}.experiment-group-dialog .experimental-group-entry{display:grid;grid:auto auto auto auto / auto 1fr 175px auto;column-gap:1rem;align-items:baseline}.experiment-variable-dialog vaadin-icon{cursor:pointer;color:var(--lumo-primary-color)}.experiment-group-dialog vaadin-icon{cursor:pointer;color:var(--lumo-primary-color)}.experiment-group-dialog .header{font-weight:700}.experiment-variable-dialog .add-new-group-action{display:flex;column-gap:1rem;color:var(--lumo-primary-color)}.experiment-variable-dialog .add-new-group-action span{cursor:pointer}.experiment-variable-dialog .content{display:flex;flex-direction:column}.experiment-group-dialog .add-new-group-action{display:flex;column-gap:1rem;color:var(--lumo-primary-color)}.experiment-group-dialog .add-new-group-action span{cursor:pointer}.experiment-group-dialog .content{display:grid;gap:1.5rem}.experiment-group-dialog .content .group-collection{display:grid;gap:1rem}.experiment-variable-dialog .content .variables{display:flex;flex-direction:column}.experiment-variable-dialog .content .variables .header{font-weight:700}.experiment-variable-dialog .content .row{display:flex;align-items:center;gap:var(--lumo-space-l)}.add-project-dialog::part(content){display:flex;flex-direction:column;height:100%;width:100%;padding:0}.add-project-dialog::part(overlay){width:66%;height:100%}.add-project-dialog::part(footer){background-color:transparent;justify-content:space-between;padding-inline:4rem}.add-project-dialog .footer-right-buttons-container{gap:var(--lumo-space-m);display:inline-flex}.add-project-dialog .collaborators-layout{width:100%;height:100%;gap:1em;display:flex;flex-direction:column}.add-project-dialog .layout-container{height:100%;padding:1rem 4rem 3rem}.add-project-dialog .experiment-information-layout{width:100%;height:100%;gap:1em;display:flex;flex-direction:column}.add-project-dialog .experiment-information-layout .experiment-name-field{width:50%}.add-project-dialog .funding-information-layout{width:100%;height:100%;gap:1em;display:flex;flex-direction:column}.add-project-dialog .funding-information-layout .funding-field .grant-label-field{min-width:15vw}.add-project-dialog .funding-information-layout .funding-field .input-fields{width:100%;gap:var(--lumo-space-xl);display:inline-flex}.add-project-dialog .project-design-layout{width:100%;height:100%;gap:1em;display:flex;flex-direction:column}.add-project-dialog .project-design-layout .description-field{height:100%;width:100%;min-height:15vh}.add-project-dialog .project-design-layout .title-field{width:100%;margin-left:var(--lumo-space-m)}.add-project-dialog .project-design-layout .code-field{max-width:20%}.add-project-dialog .project-design-layout .search-field{max-width:30%}.add-project-dialog .project-design-layout .code-and-title{width:100%;gap:var(--lumo-space-s);display:inline-flex;align-items:baseline}.change-user-details-dialog::part(overlay){min-width:fit-content;min-height:fit-content;width:50%;height:50%}.change-user-details-dialog .change-user-name{width:100%}.measurement-pooled-samples-dialog::part(overlay){min-width:50vw}.measurement-pooled-samples-dialog::part(content){display:flex;flex-direction:column;gap:var(--lumo-space-l)}.measurement-pooled-samples-dialog .pooled-measurement-details{gap:var(--lumo-space-xs);display:flex;flex-direction:column}.measurement-pooled-samples-dialog .pooled-measurement-details .pooled-detail{gap:var(--lumo-space-s);display:inline-flex}.measurement-pooled-samples-dialog .pooled-measurement-details .pooled-detail .label{color:var(--lumo-secondary-text-color);font-weight:700}.measurement-upload-dialog::part(overlay){min-width:66%;min-height:66%;max-height:66%;max-width:66%}.measurement-upload-dialog::part(content){display:flex;flex-direction:column;gap:var(--lumo-space-s)}.measurement-upload-dialog .upload-items-display,.measurement-upload-dialog .upload-items-display .upload-section{display:flex;flex-direction:column;gap:var(--lumo-space-s)}.measurement-upload-dialog .upload-items-display .upload-section .restrictions{font-size:smaller;display:inline-flex;justify-content:space-between;color:var(--lumo-contrast-60pct);column-gap:var(--lumo-space-s)}.measurement-upload-dialog .upload-items-display .uploaded-items-section .section-title{color:var(--lumo-secondary-text-color);font-size:var(--lumo-font-size-s);font-weight:500}.measurement-upload-dialog.uploaded-items-description{font-size:var(--lumo-font-size-s)}.measurement-upload-dialog .upload-items-display .uploaded-measurement-items{border:1px solid var(--lumo-contrast-20pct);border-radius:var(--lumo-border-radius-l);display:flex;flex-direction:column}.measurement-upload-dialog .upload-items-display .uploaded-measurement-items .measurement-item{border-bottom-color:var(--lumo-contrast-20pct);border-bottom-style:solid;border-width:1px;font-size:var(--lumo-font-size-s);display:flex;padding-top:var(--lumo-space-m);padding-bottom:var(--lumo-space-s);margin-inline:var(--lumo-space-m);flex-direction:column;row-gap:var(--lumo-space-m)}.measurement-upload-dialog .upload-items-display .uploaded-measurement-items .measurement-item .file-name{display:inline-flex;align-items:center;column-gap:var(--lumo-space-s)}.measurement-upload-dialog .upload-items-display .uploaded-measurement-items .measurement-item .file-icon{display:inline-flex;flex-shrink:0;font-size:smaller;color:var(--lumo-tertiary-text-color)}.measurement-upload-dialog .upload-items-display .uploaded-measurement-items .measurement-item .validation-display-box{display:flex;flex-direction:column;row-gap:var(--lumo-space-s);background-color:var(--lumo-contrast-5pct);padding-inline:var(--lumo-space-m);padding-top:var(--lumo-space-xs);padding-bottom:var(--lumo-space-xs);border-radius:var(--lumo-border-radius-m)}.measurement-upload-dialog .upload-items-display .uploaded-measurement-items .measurement-item .validation-display-box .header{font-size:small;align-items:center;display:inline-flex;column-gap:var(--lumo-space-s);font-weight:700}.measurement-upload-dialog .upload-items-display .uploaded-measurement-items .measurement-item .validation-display-box .invalid-measurement-list{padding-left:var(--lumo-space-m)}.measurement-upload-dialog .upload-items-display .uploaded-measurement-items .measurement-item:last-child{border-style:none}.measurement-upload-dialog .upload-progress-display{display:flex;flex-direction:column;gap:var(--lumo-space-s)}.measurement-upload-dialog .upload-progress-display .display-box{display:flex;flex-direction:column;gap:var(--lumo-space-s);border-radius:var(--lumo-border-radius-l);border:1px solid var(--lumo-contrast-20pct);padding:var(--lumo-space-m);margin-top:var(--lumo-space-s)}.measurement-upload-dialog .upload-progress-display .display-box .description{display:inline-flex;gap:var(--lumo-space-s);align-items:center}.project-information-dialog::part(overlay){width:66vw}.project-information-dialog .define-project-content{width:100%;height:100%;gap:1em;display:flex;flex-direction:column}.project-information-dialog .define-project-content .information,.project-information-dialog .define-project-content .contacts{display:flex;flex-direction:column}.purchase-item-upload::part(overlay){width:50%;min-width:400px}vaadin-upload-file::part(meta){font-size:smaller}.purchase-item-upload::part(content){display:flex;flex-direction:column;gap:var(--lumo-space-s)}.purchase-item-upload .restrictions{font-size:smaller;display:flex;justify-content:space-between;color:var(--lumo-contrast-60pct)}.purchase-item-upload .section-title{color:var(--lumo-secondary-text-color);font-size:var(--lumo-font-size-s);font-weight:500}.purchase-item-upload .uploaded-items-description{font-size:var(--lumo-font-size-s)}.quality-control-upload::part(content){display:flex;flex-direction:column;gap:var(--lumo-space-s)}.quality-control-upload .restrictions{font-size:smaller;display:inline-flex;justify-content:space-between;color:var(--lumo-contrast-60pct);column-gap:var(--lumo-space-s)}.quality-control-upload.section-title{color:var(--lumo-secondary-text-color);font-size:var(--lumo-font-size-s);font-weight:500}.quality-control-upload .uploaded-items-description{font-size:var(--lumo-font-size-s)}.uploaded-quality-control-items{border:1px solid var(--lumo-contrast-20pct);border-radius:var(--lumo-border-radius-l);display:flex;flex-direction:column}.uploaded-quality-control-items .quality-control-item{border-bottom-color:var(--lumo-contrast-20pct);border-bottom-style:solid;border-width:1px;font-size:var(--lumo-font-size-s);display:flex;padding-top:var(--lumo-space-m);padding-bottom:var(--lumo-space-s);margin-inline:var(--lumo-space-m);flex-direction:column;row-gap:var(--lumo-space-s)}.uploaded-quality-control-items .quality-control-item .file-name{display:inline-flex;align-items:center;column-gap:var(--lumo-space-xs)}.uploaded-quality-control-items .quality-control-item .file-icon{display:inline-flex;font-size:smaller;flex-shrink:0;color:var(--lumo-tertiary-text-color)}.uploaded-quality-control-items .quality-control-item:last-child{border-style:none}.uploaded-quality-control-items .quality-control-item .offer-icon{background-color:#4cade8;display:inline-flex;font-size:var(--lumo-font-size-xs);color:#fff;padding-inline:var(--lumo-space-xs);border-radius:var(--lumo-border-radius-s)}.uploaded-purchase-items{border:1px solid var(--lumo-contrast-20pct);border-radius:var(--lumo-border-radius-l);display:flex;flex-direction:column}.uploaded-purchase-items .purchase-item{border-bottom-color:var(--lumo-contrast-20pct);border-bottom-style:solid;border-width:1px;font-size:var(--lumo-font-size-s);display:flex;padding-top:var(--lumo-space-m);padding-bottom:var(--lumo-space-s);margin-inline:var(--lumo-space-m);flex-direction:column;row-gap:var(--lumo-space-s)}.uploaded-purchase-items .purchase-item .file-name{display:inline-flex;align-items:center;column-gap:var(--lumo-space-xs)}.uploaded-purchase-items .purchase-item .file-icon{display:inline-flex;font-size:smaller;flex-shrink:0;color:var(--lumo-tertiary-text-color)}.uploaded-purchase-items .purchase-item:last-child{border-style:none}.uploaded-purchase-items .purchase-item .signature-box{display:inline-flex;column-gap:var(--lumo-space-xs)}.uploaded-purchase-items .purchase-item .offer-icon{background-color:#4cade8;display:inline-flex;font-size:var(--lumo-font-size-xs);color:#fff;padding-inline:var(--lumo-space-xs);border-radius:var(--lumo-border-radius-s)}.validation-display-box{display:grid}
`,gl=`.navigation-button{display:flex;flex-direction:column;gap:var(--lumo-space-m);align-items:center;flex:1 1}.navigation-button .button{box-shadow:var(--lumo-box-shadow-xs)}.navigation-button .label{font-size:var(--lumo-font-size-s)}vaadin-button.primary{background-color:var(--lumo-button-primary-background-color, var(--lumo-primary-color));color:var(--lumo-button-primary-color, var(--lumo-primary-contrast-color));font-weight:600;min-width:calc(var(--lumo-button-size) * 2.5)}vaadin-button[disabled]{background-color:var(--lumo-contrast-30pct);color:var(--lumo-base-color)}vaadin-button.primary .button:before{background-color:#000}vaadin-button.primary .button:hover:before{opacity:.05}vaadin-button.tertiary{padding:0 calc(var(--lumo-button-size) / 6);background-color:transparent!important;min-width:0}vaadin-button{cursor:pointer}vaadin-button[theme~=tertiary] vaadin-icon,vaadin-button[theme~=tertiary-inline] vaadin-icon{color:var(--lumo-tertiary-text-color)}.card{background-color:var(--lumo-base-color);border:black;border-radius:var(--lumo-border-radius-m);box-shadow:var(--lumo-box-shadow-s);box-sizing:border-box;overflow:hidden;text-overflow:ellipsis;margin:4px}.creation-card{display:flex;justify-content:center;padding:1.5rem;flex:auto;max-width:300px}.creation-card>div{display:flex;flex-direction:column;justify-content:center;align-items:center;cursor:pointer}.creation-card .disclaimer-area{font-size:smaller}.creation-card .button{background-color:var(--lumo-button-primary-background-color, var(--lumo-primary-color));color:var(--lumo-button-primary-color, var(--lumo-primary-contrast-color));font-weight:600;min-width:calc(var(--lumo-button-size) * 2.5)}.creation-card .button:before{background-color:#000}.creation-card .button:hover:before{opacity:.05}.experimental-group{display:flex;flex-direction:column;flex:auto;font-size:var(--lumo-font-size-s);padding:var(--lumo-space-l);max-width:300px}.card-collection{display:flex;align-content:space-evenly;flex-flow:column;gap:1rem;margin-left:1.5rem;margin-top:1.5rem}.card-collection .collection-title{font-weight:700;color:var(--lumo-secondary-text-color);font-size:var(--lumo-font-size-m);margin-bottom:.5rem}.card-collection .collection-header{justify-content:space-between;display:flex}.card-collection .collection-content{display:flex;align-content:space-evenly;flex-flow:row wrap;gap:1rem;flex-direction:row}.card-collection .collection-controls{display:flex;align-content:space-evenly;flex-flow:row wrap;gap:.5rem}.experimental-group .header{display:flex;justify-content:space-between;align-items:baseline;margin-bottom:var(--lumo-space-m)}.experimental-group .card-title{color:var(--lumo-secondary-text-color);font-size:var(--lumo-font-size-m);font-weight:700;white-space:nowrap;margin-bottom:.5rem}.experimental-group .content{display:inline-flex;flex-wrap:wrap;gap:var(--lumo-space-m);margin-bottom:var(--lumo-space-m)}.experimental-group vaadin-icon{cursor:pointer;width:1em;color:#a9a9a9}.card.experiment-info{flex:auto;font-size:var(--lumo-font-size-s);padding:var(--lumo-space-l);max-width:300px}.card.experiment-info .title{font-size:var(--lumo-font-size-l);margin-bottom:.5rem;font-weight:700;color:var(--lumo-secondary-text-color)}.card.experiment-info .content{display:flex;flex-direction:column;justify-content:center;align-items:stretch}.experiment-info ul{list-style-type:none;display:block}.experiment-info .header{color:var(--lumo-header-text-color);font-size:var(--lumo-font-size-m);font-weight:700;display:flex;justify-content:space-between;align-items:baseline}.experiment-info .title{font-weight:700}.experiment-item{display:block;min-height:100px;width:100%}.experiment-item:hover{background-color:#f5f5f5;cursor:pointer}.experiment-item .experiment-title{font-weight:700;padding-left:1rem;color:#3f3f3f}.experiment-item .content-section{padding:1rem 1rem 0 0;display:flex;justify-content:space-between}.experiment-item .progress-section{background-color:#dcdcdc;font-size:smaller;padding-left:1rem}.experiment-item .progress-section .incomplete{background-color:#dcdcdc}.experiment-item vaadin-icon{color:#949494;min-width:var(--lumo-icon-size-m);min-height:var(--lumo-icon-size-m)}.experiment-item .active-section{padding-left:1rem;padding-bottom:var(--lumo-space-s);padding-top:var(--lumo-space-l)}.experiment-item.selected{background-color:#fff;margin-left:1rem}.experiment-item.selected .content-section{padding:1rem 2rem 0 0}.experiment-item.selected vaadin-icon{color:var(--lumo-primary-color)}.analysis-type-combo-box::part(overlay){width:20rem}.dialog-window .confirm-button:hover:before{opacity:.05}vaadin-dialog-overlay::part(content){padding:1rem 4rem 3rem}vaadin-dialog-overlay::part(footer){padding:1rem 4rem 1rem 0}vaadin-dialog-overlay::part(header){padding:2rem 4rem 1rem}vaadin-dialog-overlay::part(title){margin-inline-start:0}.notification-dialog::part(overlay){width:36.75rem}.notification-dialog .content{width:100%}.notification-dialog>[slot=header]{display:flex;align-items:center;justify-items:flex-start;gap:var(--lumo-space-s)}.notification-dialog .title{font-family:var(--lumo-font-family);line-height:var(--lumo-line-height-m);font-size:var(--lumo-font-size-xl);margin:0}.notification-dialog>[slot=header] vaadin-icon{width:1.83rem;height:1.83rem;margin:.46rem;margin-inline-start:calc(var(--lumo-space-l) - var(--lumo-space-m))}.notification-dialog .error-icon{fill:var(--lumo-error-color)}.notification-dialog .warning-icon{fill:var(--lumo-warning-color)}.notification-dialog .info-icon{fill:var(--lumo-primary-color)}.existing-groups-prevent-variable-edit .content .experimental-group-count,.existing-samples-prevent-variable-edit .content .sample-count,.existing-samples-prevent-group-edit .content .sample-count{font-weight:700}.add-personal-access-token-dialog::part(overlay){height:50%;min-width:50vw}.add-personal-access-token-dialog::part(content){display:flex;flex-direction:column}.add-personal-access-token-dialog .expiration-date{width:50%}.add-user-to-project-dialog::part(overlay){height:fit-content;min-width:fit-content}.add-user-to-project-dialog::part(content){display:flex;flex-direction:column;gap:var(--lumo-space-xl)}.add-user-to-project-dialog .person-selection-section,.add-user-to-project-dialog .role-selection-section{display:flex;flex-direction:column;gap:var(--lumo-space-s)}.add-user-to-project-dialog .section-title{color:var(--lumo-secondary-text-color);font-size:var(--lumo-font-size-l);font-weight:700}.batch-registration-dialog::part(content),.batch-update-dialog::part(content){display:flex;flex-direction:column;gap:var(--lumo-space-s)}.batch-update-dialog::part(overlay),.batch-registration-dialog::part(overlay){min-width:80vw;height:100%}.batch-update-dialog .batch-name,.batch-update-dialog .experiment-name,.batch-registration-dialog .experiment-name{margin:auto .3em;font-weight:bolder}.batch-registration-dialog .prefill-section{display:flex;justify-content:space-between;align-items:center}.batch-registration-dialog .spreadsheet-container,.batch-update-dialog .spreadsheet-container{display:flex;flex-direction:column;column-gap:var(--lumo-space-s);margin-top:var(--lumo-space-m)}.batch-registration-dialog .spreadsheet-header,.batch-update-dialog .spreadsheet-header{display:flex;justify-content:space-between;align-items:center}.batch-update-dialog .spreadsheet-controls,.batch-registration-dialog .spreadsheet-controls{display:inline-flex;column-gap:.5em;justify-content:end}.batch-update-dialog .spreadsheet-controls .error-text,.batch-registration-dialog .spreadsheet-controls .error-text{color:var(--lumo-error-text-color)}.batch-update-dialog .batch-controls>.batch-name-field,.batch-registration-dialog .batch-controls>.batch-name-field{width:calc(100% / 3)}.add-experiment-dialog::part(overlay),.edit-experiment-dialog::part(overlay){width:66vw}.edit-experiment-content,.add-experiment-content{padding-top:var(--lumo-space-m);display:flex;flex-direction:column;height:100%;width:100%;gap:var(--lumo-space-m);box-sizing:border-box}.edit-experiment-content .header,.add-experiment-content .header{font-weight:700}.edit-experiment-content .full-width-input,.add-experiment-content .full-width-input{width:100%}.input-with-icon-selection{display:inline-flex;column-gap:1rem}.icon-and-component{display:inline-flex;justify-content:start;align-items:flex-end;column-gap:var(--lumo-space-m)}.box-flexgrow{flex-grow:1}.edit-project-dialog .content{max-width:60vw;padding-bottom:1em}.edit-project-dialog::part(overlay){width:66vw}.edit-project-dialog .funding-field .input-fields,.contact-field .input-fields{display:flex;gap:1em;align-items:baseline}.contact-field .contact-selection,.contact-field .input-fields,.contact-field .input-fields>*{width:100%}.contact-field .prefill-input-fields{line-height:0;display:flex;flex-direction:column;align-items:baseline}.contact-field .prefill-input-fields .contact-self-select{width:36em}.edit-project-dialog .form-content .contact-field .name-field,.edit-project-dialog .form-content .contact-field .email-field{flex-grow:1}.edit-project-dialog .project-contacts{display:flex;gap:1em;flex-direction:column;flex-wrap:wrap;margin:0;padding:var(--lumo-space-m) 0 0}.experiment-group-dialog::part(overlay){width:66%}.experiment-group-dialog .number-field{min-width:175px}.experiment-group-dialog .text-field{min-width:100px}.experiment-group-dialog .experimental-group-entry{display:grid;grid:auto auto auto auto / auto 1fr 175px auto;column-gap:1rem;align-items:baseline}.experiment-variable-dialog vaadin-icon{cursor:pointer;color:var(--lumo-primary-color)}.experiment-group-dialog vaadin-icon{cursor:pointer;color:var(--lumo-primary-color)}.experiment-group-dialog .header{font-weight:700}.experiment-variable-dialog .add-new-group-action{display:flex;column-gap:1rem;color:var(--lumo-primary-color)}.experiment-variable-dialog .add-new-group-action span{cursor:pointer}.experiment-variable-dialog .content{display:flex;flex-direction:column}.experiment-group-dialog .add-new-group-action{display:flex;column-gap:1rem;color:var(--lumo-primary-color)}.experiment-group-dialog .add-new-group-action span{cursor:pointer}.experiment-group-dialog .content{display:grid;gap:1.5rem}.experiment-group-dialog .content .group-collection{display:grid;gap:1rem}.experiment-variable-dialog .content .variables{display:flex;flex-direction:column}.experiment-variable-dialog .content .variables .header{font-weight:700}.experiment-variable-dialog .content .row{display:flex;align-items:center;gap:var(--lumo-space-l)}.add-project-dialog::part(content){display:flex;flex-direction:column;height:100%;width:100%;padding:0}.add-project-dialog::part(overlay){width:66%;height:100%}.add-project-dialog::part(footer){background-color:transparent;justify-content:space-between;padding-inline:4rem}.add-project-dialog .footer-right-buttons-container{gap:var(--lumo-space-m);display:inline-flex}.add-project-dialog .collaborators-layout{width:100%;height:100%;gap:1em;display:flex;flex-direction:column}.add-project-dialog .layout-container{height:100%;padding:1rem 4rem 3rem}.add-project-dialog .experiment-information-layout{width:100%;height:100%;gap:1em;display:flex;flex-direction:column}.add-project-dialog .experiment-information-layout .experiment-name-field{width:50%}.add-project-dialog .funding-information-layout{width:100%;height:100%;gap:1em;display:flex;flex-direction:column}.add-project-dialog .funding-information-layout .funding-field .grant-label-field{min-width:15vw}.add-project-dialog .funding-information-layout .funding-field .input-fields{width:100%;gap:var(--lumo-space-xl);display:inline-flex}.add-project-dialog .project-design-layout{width:100%;height:100%;gap:1em;display:flex;flex-direction:column}.add-project-dialog .project-design-layout .description-field{height:100%;width:100%;min-height:15vh}.add-project-dialog .project-design-layout .title-field{width:100%;margin-left:var(--lumo-space-m)}.add-project-dialog .project-design-layout .code-field{max-width:20%}.add-project-dialog .project-design-layout .search-field{max-width:30%}.add-project-dialog .project-design-layout .code-and-title{width:100%;gap:var(--lumo-space-s);display:inline-flex;align-items:baseline}.change-user-details-dialog::part(overlay){min-width:fit-content;min-height:fit-content;width:50%;height:50%}.change-user-details-dialog .change-user-name{width:100%}.measurement-pooled-samples-dialog::part(overlay){min-width:50vw}.measurement-pooled-samples-dialog::part(content){display:flex;flex-direction:column;gap:var(--lumo-space-l)}.measurement-pooled-samples-dialog .pooled-measurement-details{gap:var(--lumo-space-xs);display:flex;flex-direction:column}.measurement-pooled-samples-dialog .pooled-measurement-details .pooled-detail{gap:var(--lumo-space-s);display:inline-flex}.measurement-pooled-samples-dialog .pooled-measurement-details .pooled-detail .label{color:var(--lumo-secondary-text-color);font-weight:700}.measurement-upload-dialog::part(overlay){min-width:66%;min-height:66%;max-height:66%;max-width:66%}.measurement-upload-dialog::part(content){display:flex;flex-direction:column;gap:var(--lumo-space-s)}.measurement-upload-dialog .upload-items-display,.measurement-upload-dialog .upload-items-display .upload-section{display:flex;flex-direction:column;gap:var(--lumo-space-s)}.measurement-upload-dialog .upload-items-display .upload-section .restrictions{font-size:smaller;display:inline-flex;justify-content:space-between;color:var(--lumo-contrast-60pct);column-gap:var(--lumo-space-s)}.measurement-upload-dialog .upload-items-display .uploaded-items-section .section-title{color:var(--lumo-secondary-text-color);font-size:var(--lumo-font-size-s);font-weight:500}.measurement-upload-dialog.uploaded-items-description{font-size:var(--lumo-font-size-s)}.measurement-upload-dialog .upload-items-display .uploaded-measurement-items{border:1px solid var(--lumo-contrast-20pct);border-radius:var(--lumo-border-radius-l);display:flex;flex-direction:column}.measurement-upload-dialog .upload-items-display .uploaded-measurement-items .measurement-item{border-bottom-color:var(--lumo-contrast-20pct);border-bottom-style:solid;border-width:1px;font-size:var(--lumo-font-size-s);display:flex;padding-top:var(--lumo-space-m);padding-bottom:var(--lumo-space-s);margin-inline:var(--lumo-space-m);flex-direction:column;row-gap:var(--lumo-space-m)}.measurement-upload-dialog .upload-items-display .uploaded-measurement-items .measurement-item .file-name{display:inline-flex;align-items:center;column-gap:var(--lumo-space-s)}.measurement-upload-dialog .upload-items-display .uploaded-measurement-items .measurement-item .file-icon{display:inline-flex;flex-shrink:0;font-size:smaller;color:var(--lumo-tertiary-text-color)}.measurement-upload-dialog .upload-items-display .uploaded-measurement-items .measurement-item .validation-display-box{display:flex;flex-direction:column;row-gap:var(--lumo-space-s);background-color:var(--lumo-contrast-5pct);padding-inline:var(--lumo-space-m);padding-top:var(--lumo-space-xs);padding-bottom:var(--lumo-space-xs);border-radius:var(--lumo-border-radius-m)}.measurement-upload-dialog .upload-items-display .uploaded-measurement-items .measurement-item .validation-display-box .header{font-size:small;align-items:center;display:inline-flex;column-gap:var(--lumo-space-s);font-weight:700}.measurement-upload-dialog .upload-items-display .uploaded-measurement-items .measurement-item .validation-display-box .invalid-measurement-list{padding-left:var(--lumo-space-m)}.measurement-upload-dialog .upload-items-display .uploaded-measurement-items .measurement-item:last-child{border-style:none}.measurement-upload-dialog .upload-progress-display{display:flex;flex-direction:column;gap:var(--lumo-space-s)}.measurement-upload-dialog .upload-progress-display .display-box{display:flex;flex-direction:column;gap:var(--lumo-space-s);border-radius:var(--lumo-border-radius-l);border:1px solid var(--lumo-contrast-20pct);padding:var(--lumo-space-m);margin-top:var(--lumo-space-s)}.measurement-upload-dialog .upload-progress-display .display-box .description{display:inline-flex;gap:var(--lumo-space-s);align-items:center}.project-information-dialog::part(overlay){width:66vw}.project-information-dialog .define-project-content{width:100%;height:100%;gap:1em;display:flex;flex-direction:column}.project-information-dialog .define-project-content .information,.project-information-dialog .define-project-content .contacts{display:flex;flex-direction:column}.purchase-item-upload::part(overlay){width:50%;min-width:400px}vaadin-upload-file::part(meta){font-size:smaller}.purchase-item-upload::part(content){display:flex;flex-direction:column;gap:var(--lumo-space-s)}.purchase-item-upload .restrictions{font-size:smaller;display:flex;justify-content:space-between;color:var(--lumo-contrast-60pct)}.purchase-item-upload .section-title{color:var(--lumo-secondary-text-color);font-size:var(--lumo-font-size-s);font-weight:500}.purchase-item-upload .uploaded-items-description{font-size:var(--lumo-font-size-s)}.quality-control-upload::part(content){display:flex;flex-direction:column;gap:var(--lumo-space-s)}.quality-control-upload .restrictions{font-size:smaller;display:inline-flex;justify-content:space-between;color:var(--lumo-contrast-60pct);column-gap:var(--lumo-space-s)}.quality-control-upload.section-title{color:var(--lumo-secondary-text-color);font-size:var(--lumo-font-size-s);font-weight:500}.quality-control-upload .uploaded-items-description{font-size:var(--lumo-font-size-s)}.uploaded-quality-control-items{border:1px solid var(--lumo-contrast-20pct);border-radius:var(--lumo-border-radius-l);display:flex;flex-direction:column}.uploaded-quality-control-items .quality-control-item{border-bottom-color:var(--lumo-contrast-20pct);border-bottom-style:solid;border-width:1px;font-size:var(--lumo-font-size-s);display:flex;padding-top:var(--lumo-space-m);padding-bottom:var(--lumo-space-s);margin-inline:var(--lumo-space-m);flex-direction:column;row-gap:var(--lumo-space-s)}.uploaded-quality-control-items .quality-control-item .file-name{display:inline-flex;align-items:center;column-gap:var(--lumo-space-xs)}.uploaded-quality-control-items .quality-control-item .file-icon{display:inline-flex;font-size:smaller;flex-shrink:0;color:var(--lumo-tertiary-text-color)}.uploaded-quality-control-items .quality-control-item:last-child{border-style:none}.uploaded-quality-control-items .quality-control-item .offer-icon{background-color:#4cade8;display:inline-flex;font-size:var(--lumo-font-size-xs);color:#fff;padding-inline:var(--lumo-space-xs);border-radius:var(--lumo-border-radius-s)}.uploaded-purchase-items{border:1px solid var(--lumo-contrast-20pct);border-radius:var(--lumo-border-radius-l);display:flex;flex-direction:column}.uploaded-purchase-items .purchase-item{border-bottom-color:var(--lumo-contrast-20pct);border-bottom-style:solid;border-width:1px;font-size:var(--lumo-font-size-s);display:flex;padding-top:var(--lumo-space-m);padding-bottom:var(--lumo-space-s);margin-inline:var(--lumo-space-m);flex-direction:column;row-gap:var(--lumo-space-s)}.uploaded-purchase-items .purchase-item .file-name{display:inline-flex;align-items:center;column-gap:var(--lumo-space-xs)}.uploaded-purchase-items .purchase-item .file-icon{display:inline-flex;font-size:smaller;flex-shrink:0;color:var(--lumo-tertiary-text-color)}.uploaded-purchase-items .purchase-item:last-child{border-style:none}.uploaded-purchase-items .purchase-item .signature-box{display:inline-flex;column-gap:var(--lumo-space-xs)}.uploaded-purchase-items .purchase-item .offer-icon{background-color:#4cade8;display:inline-flex;font-size:var(--lumo-font-size-xs);color:#fff;padding-inline:var(--lumo-space-xs);border-radius:var(--lumo-border-radius-s)}.validation-display-box{display:grid}.card-deck{display:flex;align-content:space-evenly;flex-flow:row wrap}.disclaimer{display:flex;justify-content:center;width:100%;height:100%}.disclaimer .disclaimer-content{display:flex;flex-direction:column;justify-content:center;align-items:center;gap:var(--lumo-space-s)}.disclaimer .disclaimer-label{font-weight:400;color:#2d2d2d;font-size:var(--lumo-font-size-s)}.disclaimer .disclaimer-title{font-weight:700;color:#2d2d2d;font-size:var(--lumo-font-size-m);margin-bottom:.5rem}.grid-details-item{display:flex;flex-direction:column;flex-wrap:wrap;gap:var(--lumo-space-s);cursor:default}.grid-details-item .entry{display:inline-flex;gap:var(--lumo-space-s)}.grid-details-item .entry .entry-label{font-weight:700;white-space:nowrap}.grid-details-item .entry .entry-value-list{display:flex;flex-wrap:wrap;gap:var(--lumo-space-s)}.grid-details-item .entry-value{display:inline-flex;flex-wrap:wrap;word-break:break-all}.ontology-component{display:flex;flex-direction:column;gap:var(--lumo-space-s)}.project-creation-stepper-arrow{align-items:end;color:var(--lumo-contrast-60pct);display:flex;margin-inline:var(--lumo-space-l)}.stepper{margin-top:.5rem;display:inline-flex;padding-inline:4rem}.stepper .step{display:flex;flex-direction:column;align-items:center;white-space:nowrap;gap:var(--lumo-space-s)}.stepper .step[selected]>vaadin-avatar{background-color:var(--lumo-primary-color-50pct);color:#fff}.stepper .step>vaadin-avatar{background-color:var(--lumo-contrast-60pct);color:#fff}.ontology-entry-collection{display:inline-flex;flex-wrap:wrap;gap:var(--lumo-space-s);width:100%;white-space:nowrap}.ontology-entry-collection .ontology-entry{display:inline-flex;gap:var(--lumo-space-s);width:100%}.project-role-item .project-role-label{font-weight:700}.project-role-item .project-role-description{display:flex;flex-direction:column}.tag-collection{display:inline-flex;flex-wrap:wrap;gap:var(--lumo-space-xs);white-space:nowrap}img.clickable{cursor:pointer}.info-section{width:100%;display:grid;gap:1rem}.info-section>.info-title{font-weight:700;color:var(--lumo-secondary-text-color);font-size:var(--lumo-font-size-xl)}.info-section .info-entry-label{font-weight:700;font-size:var(--lumo-font-size-s);color:var(--lumo-secondary-text-color);display:flex;gap:var(--lumo-space-s)}.info-entry{display:grid;grid:auto / 20% auto}.info-content{display:grid;gap:.75rem}.project-information-content{display:grid;gap:4rem}.contact-item,.person-contact-display{display:grid}.contact-item .contact-email,.person-contact-display .email{font-size:var(--lumo-font-size-s);color:var(--lumo-secondary-text-color)}vaadin-icon.primary{color:var(--lumo-primary-color)}vaadin-icon.error{color:var(--lumo-error-color)}vaadin-icon.success{color:var(--lumo-success-color)}vaadin-icon.clickable{cursor:pointer}vaadin-icon.small{width:var(--lumo-icon-size-s)}vaadin-icon.smallest{width:1em}vaadin-icon.copy-icon{color:var(--lumo-contrast-50pct)}#content-area{grid-area:content-area}#main-layout{display:grid;grid-template-columns:1fr;grid-template-rows:minmax(max-content,95%) minmax(min-content,5%);height:100%;grid-template-areas:"content-area" "data-manager-footer"}#landing-page-layout .landing-page-content{background-size:cover;background-position:center;background-repeat:no-repeat;height:100%}#landing-page-layout .landing-page-title-and-logo{display:flex;align-items:center;justify-content:center;flex-direction:column;row-gap:var(--lumo-space-s);margin-bottom:var(--lumo-space-l);padding-top:var(--lumo-space-xl)}#landing-page-layout .landing-page-title-and-logo .title{font-weight:700;font-size:var(--lumo-font-size-xxl)}#landing-page-layout .landing-page-title-and-logo .subtitle{color:var(--lumo-tertiary-text-color);font-weight:700}#landing-page-layout .landing-page-title-and-logo .ut-logo{height:2.5em;margin-bottom:var(--lumo-space-xl)}#data-manager-footer{display:inline-flex;column-gap:var(--lumo-space-l);border-top:thin solid var(--lumo-contrast-10pct);padding-inline:var(--lumo-space-m);padding-bottom:var(--lumo-space-s);padding-top:var(--lumo-space-s);grid-area:data-manager-footer}.main{display:grid;height:100%}.main .title{font-weight:700;color:var(--lumo-secondary-text-color);font-size:var(--lumo-font-size-xxl);margin-bottom:.5rem}.main.experiment{grid-template-columns:minmax(max-content,100%);grid-template-rows:auto;grid-template-areas:"experimentdetails"}.main.experiment .experiment-details-component{grid-area:experimentdetails}.main.legal-notice{padding:var(--lumo-space-xl);height:fit-content;width:clamp(700px,60vw,100%);display:flex;margin:auto}.main.data-privacy-agreement{padding:var(--lumo-space-xl);height:fit-content;width:clamp(700px,50vw,100%);display:flex;margin:auto}.main.measurement{grid-template-columns:minmax(max-content,70%) minmax(max-content,30%);grid-template-rows:minmax(max-content,25%) minmax(max-content,70%);grid-template-areas:". measurementtemplatelist" "measurementdetails measurementdetails"}.main.measurement .page-area{border:1px solid;border-color:var(--lumo-contrast-10pct);border-radius:var(--lumo-border-radius-m)}.main.measurement .no-samples-registered-disclaimer,.main.measurement .no-measurements-registered-disclaimer{grid-column-start:1;grid-column-end:-1;grid-row-start:1;grid-row-end:-1}.main.measurement .no-measurements-registered-disclaimer{display:flex;flex-direction:column;row-gap:var(--lumo-space-m);align-items:center;justify-content:center}.main.measurement .no-measurements-registered-disclaimer .no-measurement-registered-title{font-weight:700;font-size:var(--lumo-font-size-m);margin-bottom:.5rem}.main.measurement .no-measurements-registered-disclaimer .no-measurement-registered-content{display:flex;flex-direction:column;align-items:center}.main.measurement .measurement-template-list-component{grid-area:measurementtemplatelist}.main.measurement .measurement-details-component{grid-area:measurementdetails}.main.measurement .measurement-main-content{display:flex;flex-direction:column;row-gap:var(--lumo-space-m);padding:var(--lumo-space-m)}.main.measurement .measurement-main-content .title{font-weight:700;color:var(--lumo-secondary-text-color);font-size:var(--lumo-font-size-xxl);margin-bottom:.5rem}.main.measurement .measurement-main-content .buttonsAndInfo{display:flex;flex-direction:column;row-gap:var(--lumo-space-m)}.main.measurement .measurement-main-content .buttonsAndInfo .info{border-radius:var(--lumo-border-radius-m);column-gap:var(--lumo-space-s);background-color:var(--lumo-contrast-5pct);align-items:center;width:fit-content;padding-inline:var(--lumo-space-m);padding-top:var(--lumo-space-s);padding-bottom:var(--lumo-space-s)}.main.measurement .measurement-main-content .buttonsAndInfo .buttonAndField{display:inline-flex;justify-content:space-between;margin-top:auto}.main.measurement .measurement-main-content .button-bar{display:inline-flex;column-gap:var(--lumo-space-s)}.main.ontology-lookup-main{grid-template-columns:auto;grid-template-rows:auto;grid-template-areas:"ontologylookupcomponent"}.main.ontology-lookup-component{grid-area:ontologylookupcomponent}.main.project{grid-template-columns:minmax(min-content,80%) minmax(min-content,25%);grid-template-rows:auto;grid-template-areas:"projectdetails experimentlist" "projectdetails offerlist" "projectdetails qualitycontrollist"}.main.project .page-area{border:1px solid;border-color:var(--lumo-contrast-10pct);border-radius:var(--lumo-border-radius-m)}.main.project .project-details-component{grid-area:projectdetails}.main.project .experiment-list-component{grid-area:experimentlist}.main.project .offer-list-component{grid-area:offerlist}.main.project .quality-control-list-component{grid-area:qualitycontrollist}.main.project-overview{grid-template-columns:minmax(min-content,1fr);grid-template-rows:minmax(min-content,10%) minmax(min-content,80%);grid-template-areas:"." "projectcollection"}.main.project-overview .title-and-description{padding:var(--lumo-space-m);flex-direction:column;display:flex;gap:var(--lumo-space-s);white-space:pre-line}.main.project-overview .project-overview-title{font-weight:700;color:var(--lumo-secondary-text-color);font-size:var(--lumo-font-size-xxl);margin-bottom:.5rem}.main .project-overview .project-collection-component{grid-area:projectcollection}.main.raw-data{grid-template-columns:minmax(min-content,80%) minmax(min-content,20%);grid-template-rows:minmax(min-content,20%) minmax(min-content,80%);grid-template-areas:". rawdatadownloadinformation" "rawdatadetails rawdatadownloadinformation"}.main.raw-data .page-area{border:1px solid;border-color:var(--lumo-contrast-10pct);border-radius:var(--lumo-border-radius-m)}.main.raw-data .no-measurements-registered-disclaimer,.main.raw-data .no-raw-data-registered-disclaimer{grid-column-start:1;grid-column-end:-1;grid-row-start:1;grid-row-end:-1}.main.raw-data .raw-data-download-information-component{grid-area:rawdatadownloadinformation}.main.raw-data .raw-data-details-component{grid-area:rawdatadetails}.main.raw-data .raw-data-main-content{display:flex;flex-direction:column;row-gap:var(--lumo-space-m);padding:var(--lumo-space-m)}.main.raw-data .raw-data-main-content .title{font-weight:700;color:var(--lumo-secondary-text-color);font-size:var(--lumo-font-size-xxl);margin-bottom:.5rem}.main.raw-data .raw-data-main-content .buttonAndField{display:inline-flex;justify-content:space-between;margin-top:auto}.main.sample{grid-template-columns:minmax(min-content,60%) minmax(min-content,50%);grid-template-rows:minmax(min-content,20%) minmax(min-content,75%);grid-template-areas:". batchdetails" "sampledetails sampledetails"}.main.sample .page-area{border:1px solid;border-color:var(--lumo-contrast-10pct);border-radius:var(--lumo-border-radius-m)}.main.sample .batch-details-component{grid-area:batchdetails}.main.sample .sample-details-component{grid-area:sampledetails}.main.user-profile{grid-template-columns:minmax(min-content,auto);grid-template-rows:minmax(min-content,auto);grid-template-areas:"user-profile-component"}@media only screen and (max-width: 1200px){.main.experiment{grid-template-columns:minmax(min-content,1fr);grid-template-areas:"experimentdetails";grid-template-rows:minmax(min-content,1fr)}.main.measurement{grid-template-columns:minmax(min-content,1fr);grid-template-areas:"." "measurementtemplatelist" "measurementdetails";grid-template-rows:minmax(min-content,20%) minmax(min-content,20%) minmax(min-content,60%)}.main.project{grid-template-columns:minmax(min-content,1fr);grid-template-areas:"projectdetails" "experimentlist" "offerlist" "qualitycontrollist";grid-template-rows:minmax(min-content,60%) minmax(min-content,20%) minmax(min-content,20%)}.main.project-overview{grid-template-columns:minmax(min-content,1fr);grid-template-rows:minmax(min-content,10%) minmax(min-content,80%);grid-template-areas:"." "projectcollection"}.main.raw-data{grid-template-columns:minmax(min-content,1fr);grid-template-areas:"." "rawdatadownloadinformation" "rawdatadetails";grid-template-rows:minmax(min-content,20%) minmax(min-content,20%) minmax(min-content,60%)}.main.sample{grid-template-columns:minmax(min-content,1fr);grid-template-areas:"batchdetails" "sampledetails";grid-template-rows:minmax(min-content,20%) minmax(min-content,75%)}}.data-manager-layout::part(navbar){display:inline-flex;justify-content:space-between;background-image:linear-gradient(var(--lumo-contrast-5pct),var(--lumo-contrast-5pct))}.data-manager-layout .data-manager-title{font-size:var(--lumo-font-size-l);color:var(--lumo-header-text-color);font-weight:600;line-height:var(--lumo-line-height-xs);margin-block:0}.data-manager-menu{display:inline-flex;justify-content:space-between;column-gap:var(--lumo-space-s)}.data-manager-menu .menubar::part(container){display:inline-flex;column-gap:var(--lumo-space-s)}.data-manager-menu .user-avatar{cursor:pointer}.experiment-main-layout .experiment-app-navbar{display:inline-flex;justify-content:space-between;padding-right:var(--lumo-space-m)}.drawer-title-bar{display:inline-flex;align-items:center}.experiment-navigation-component{width:100%;padding-top:var(--lumo-space-s);padding-bottom:var(--lumo-space-s);background-color:var(--lumo-base-color)}.experiment-navigation-component .experiment-navigation-tabs{width:100%}.experiment-navigation-component .experiment-navigation-tabs .arrow-tab{align-items:end;color:var(--lumo-contrast-60pct)}.project-main-layout .project-main-layout-navbar{display:inline-flex;justify-content:space-between;width:100%;padding-right:var(--lumo-space-m);align-items:center}.experiment-main-layout .experiment-main-layout-navbar{padding-top:var(--lumo-space-s);padding-bottom:var(--lumo-space-s);display:inline-flex;justify-content:space-between;padding-right:var(--lumo-space-m);align-items:center}.project-navbar-title{font-weight:700;font-size:var(--lumo-font-size-l);display:inline-flex;align-items:inherit;column-gap:var(--lumo-space-s)}.experiment-main-layout .experiment-main-layout-navbar-container{display:flex;flex-direction:column;width:100%}#landing-page-layout::part(navbar){padding-inline:var(--lumo-space-m)}.user-main-layout::part(navbar){padding-inline:var(--lumo-space-m)}.navbar-title{font-weight:700;display:inline-flex;align-items:inherit;column-gap:var(--lumo-space-s)}.project-navigation-drawer{display:flex;flex-direction:column;gap:var(--lumo-space-m);margin-inline:var(--lumo-space-s)}.project-navigation-drawer-title{font-weight:700;font-size:var(--lumo-font-size-xl);margin-left:var(--lumo-space-s);margin-top:var(--lumo-space-s);margin-bottom:var(--lumo-space-l)}.project-navigation-drawer vaadin-side-nav-item.primary::part(item),.project-navigation-drawer vaadin-side-nav-item.primary [slot=prefix],.project-navigation-drawer vaadin-side-nav-item.primary [slot=suffix],.project-navigation-drawer vaadin-side-nav-item::part(toggle-button){color:var(--lumo-primary-color)}.project-navigation-drawer vaadin-side-nav-item.hoverable::part(item):hover{background-color:var(--lumo-primary-color-10pct)}.project-navigation-drawer .content{display:inline-flex;flex-direction:column;gap:var(--lumo-space-l)}.project-navigation-drawer .project-section{display:flex;flex-direction:column}.project-navigation-drawer .project-items{display:flex;flex-direction:column;gap:var(--lumo-space-m)}.project-navigation-drawer .section-divider{margin-top:var(--lumo-space-m);margin-bottom:var(--lumo-space-m)}.project-selection-menu{margin-left:var(--lumo-space-s);margin-right:var(--lumo-space-s)}.project-selection-menu vaadin-menu-bar-button[aria-haspopup]{overflow:hidden;text-overflow:ellipsis;width:98%}.project-selection-menu vaadin-menu-bar-item::part(content){width:100%}.recent-projects-header{color:var(--lumo-tertiary-text-color);margin-left:var(--lumo-space-l)}.dropdown-field{display:inline-flex;justify-content:space-between;width:100%}.selected-project-title{text-overflow:ellipsis;overflow:hidden}.page{height:100%;width:100%}.page-area{background-color:var(--lumo-base-color);padding:var(--lumo-space-m);flex-direction:column;display:flex;gap:var(--lumo-space-s)}.page-area .title{font-weight:700;color:var(--lumo-secondary-text-color);font-size:var(--lumo-font-size-xxl);margin-bottom:.5rem}.page-area.navbar{justify-content:space-evenly;display:flex;flex-direction:row;align-items:center;gap:var(--lumo-space-xs)}.batch-details-component .editor-buttons{display:inline-flex;gap:var(--lumo-space-s)}.batch-details-component .title-and-controls{display:inline-flex;justify-content:space-between;align-items:baseline}.experiment-details-component .sample-source-display{flex-direction:row;display:flex;justify-content:space-between}.experiment-details-component .sample-source-display .sample-source{flex-direction:column;display:flex;row-gap:var(--lumo-space-m)}.experiment-details-component .sample-source-display .sample-source .header{display:inline-flex;justify-content:start;align-items:start;column-gap:var(--lumo-space-m);font-size:var(--lumo-font-size-m);font-weight:500}.experiment-details-component .sample-source-display .sample-source .ontologies{display:flex;row-gap:var(--lumo-space-s);align-items:center;width:100%;flex-direction:column}.experiment-details-component .sample-source-display .sample-source .ontologies .ontology{display:inline-flex;gap:var(--lumo-space-s);align-items:center;width:100%}.experiment-details-component .sample-source-display .icon-with-list vaadin-icon{color:var(--lumo-primary-color);margin-right:1em;margin-top:.5em}.experiment-details-component .header{display:flex;justify-content:space-between;align-items:baseline}.experiment-details-component .details-content{padding:var(--lumo-space-m);display:flex;flex-direction:column;flex-grow:1;gap:var(--lumo-space-m);font-size:var(--lumo-font-size-s)}.experiment-details-component vaadin-tabsheet{height:100%;width:100%}.experiment-details-component .details-content .experimental-groups-container,.experiment-details-component .details-content .experimental-variables-container{height:100%;width:100%}.experiment-details-component .sample-registration-possible{margin-bottom:var(--lumo-space-m)}.experiment-list-component .content{display:grid;gap:var(--lumo-space-m);grid-auto-rows:max-content}.experiment-list-component .header{display:flex;justify-content:space-between;align-items:center;gap:var(--lumo-space-m)}.measurement-details-component{height:100%;justify-content:center;align-items:center}.measurement-details-component .measurement-tabsheet{height:100%;width:100%}.measurement-details-component .measurement-grid{height:100%}.measurement-details-component .measurement-grid .measurement-column-cell,.measurement-details-component .measurement-grid .sample-column-cell{display:inline-flex;column-gap:var(--lumo-space-s);align-items:center}.measurement-details-component .measurement-grid .sample-column-cell .expand-icon{width:1em;height:1em;color:var(--lumo-primary-color);cursor:pointer}.measurement-details-component .measurement-grid .organisation-entry{align-items:center;column-gap:var(--lumo-space-s);display:flex}.measurement-details-component .measurement-grid .organisation-entry .organisation-icon{min-width:var(--lumo-icon-size-m);min-height:var(--lumo-icon-size-m)}.measurement-details-component .measurement-grid .instrument-column{column-gap:var(--lumo-space-s);align-items:center;display:inline-flex}.measurement-details-component .measurement-grid .measurement-item{display:flex;flex-direction:column;row-gap:var(--lumo-space-s);cursor:default}.measurement-details-component .measurement-grid .measurement-item .entry{display:inline-flex;column-gap:var(--lumo-space-s)}.measurement-details-component .measurement-grid .measurement-item .entry .entry-label{font-weight:700;white-space:nowrap}.ontology-lookup-component{display:flex;flex-direction:column;row-gap:var(--lumo-space-m)}.ontology-lookup-component .search-field{max-width:33%}.ontology-lookup-component .ontology-grid-section{border-top:1px solid var(--lumo-contrast-10pct);margin-right:var(--lumo-space-xl);row-gap:var(--lumo-space-s);padding-top:var(--lumo-space-m);height:100%;display:flex;flex-direction:column}.ontology-lookup-component .ontology-grid-section .ontology-grid{height:100%;margin-top:var(--lumo-space-s)}.ontology-lookup-component .ontology-grid vaadin-grid-cell-content{padding:var(--lumo-space-xs)}.ontology-lookup-component .ontology-item{border:black;border-radius:var(--lumo-border-radius-m);box-shadow:var(--lumo-box-shadow-s);box-sizing:border-box;display:flex;flex-direction:column;flex-wrap:wrap;margin-bottom:var(--lumo-space-s);margin-top:var(--lumo-space-s);overflow:hidden;padding:var(--lumo-space-m);row-gap:var(--lumo-space-s);text-overflow:ellipsis;white-space:normal}.ontology-lookup-component .ontology-item .header{display:inline-flex;column-gap:var(--lumo-space-s);align-items:center}.ontology-lookup-component .ontology-item .header .copy-icon{display:inline-flex;font-size:smaller;flex-shrink:0;color:var(--lumo-tertiary-text-color)}.copy-icon-success{display:inline-flex;font-size:smaller;flex-shrink:0;color:var(--lumo-success-color)}.success-background-hue{background-color:var(--lumo-success-color-10pct)}.base-background{background-color:var(--lumo-base-color)}.ontology-lookup-component .ontology-item .ontology-item-title{font-weight:700;color:var(--lumo-secondary-text-color);font-size:var(--lumo-font-size-l)}.ontology-lookup-component .ontology-item .url{display:inline-flex;font-size:smaller;flex-shrink:0;color:var(--lumo-tertiary-text-color)}.personal-access-token-component{row-gap:var(--lumo-space-l);display:flex}.personal-access-token-component .header{justify-content:space-between;display:inline-flex}.personal-access-token-component .header .buttons{column-gap:var(--lumo-space-s);display:inline-flex}.personal-access-token-component .description{display:flex;flex-direction:column;row-gap:var(--lumo-space-m)}.personal-access-token-component .personal-access-token-container{width:60%;height:60%}.personal-access-token-component .personal-access-token-container .personal-access-token-list{border:1px solid var(--lumo-contrast-20pct)}.personal-access-token-component .personal-access-token-container .show-created-personal-access-token-details{display:flex;flex-direction:column;row-gap:var(--lumo-space-s);padding:var(--lumo-space-m);border:1px solid var(--lumo-contrast-20pct);width:100%}.personal-access-token-component .personal-access-token-container .show-created-personal-access-token-layout{display:flex;justify-content:space-between;align-items:center}.personal-access-token-component .personal-access-token-container .show-created-personal-access-token-details .copy-disclaimer{display:inline-flex;column-gap:var(--lumo-space-s);font-size:var(--lumo-font-size-s);align-items:center}.personal-access-token-component .personal-access-token-container .show-created-personal-access-token-details .token-text{display:inline-flex;column-gap:var(--lumo-space-s);align-items:center}.personal-access-token-component .personal-access-token-container .show-encrypted-personal-access-token-details{display:flex;flex-direction:column;row-gap:var(--lumo-space-m)}.personal-access-token-component .personal-access-token-container .show-encrypted-personal-access-token-details .copy-disclaimer,.personal-access-token-component .personal-access-token-container .show-encrypted-personal-access-token-details .expiration-date,.personal-access-token-component .personal-access-token-container .show-encrypted-personal-access-token-details .token-text{display:inline-flex;column-gap:var(--lumo-space-s);align-items:center}.personal-access-token-component .personal-access-token-container .show-encrypted-personal-access-token-layout{display:flex;padding:var(--lumo-space-m);border-bottom:1px solid var(--lumo-contrast-20pct);justify-content:space-between;align-items:center}.project-access-component{display:flex;row-gap:var(--lumo-space-l);flex-direction:column;width:clamp(500px,70%,100%);height:max-content}.project-access-component .header{display:inline-flex;justify-content:space-between;align-items:baseline}.project-access-component .change-project-access-cell{display:inline-flex;column-gap:var(--lumo-space-l);align-items:center}.project-collection-component{height:100%;border-top:none}.project-collection-component .header{display:flex;column-gap:var(--lumo-space-xl);flex-direction:column}.project-collection-component .controls{display:flex;align-content:space-between;gap:1rem}.project-collection-component .project-grid{height:100%;margin-top:var(--lumo-space-s)}.project-collection-component .project-grid vaadin-grid-cell-content{padding:var(--lumo-space-xs)}.project-collection-component .project-overview-item{border:black;border-radius:var(--lumo-border-radius-m);box-shadow:var(--lumo-box-shadow-s);box-sizing:border-box;display:flex;flex-direction:column;flex-wrap:wrap;margin-bottom:var(--lumo-space-s);margin-top:var(--lumo-space-s);overflow:hidden;padding:var(--lumo-space-l);row-gap:var(--lumo-space-s);text-overflow:ellipsis;white-space:normal;cursor:pointer}.project-collection-component .project-overview-item .header{display:inline-flex;column-gap:var(--lumo-space-s);flex-direction:row}.project-collection-component .project-overview-item .project-overview-item-title{font-weight:700;color:var(--lumo-secondary-text-color);font-size:var(--lumo-font-size-l)}.project-collection-component .project-overview-item .details{display:flex;flex-direction:column;row-gap:var(--lumo-space-s);color:var(--lumo-secondary-text-color)}.project-details-component .header{display:flex;justify-content:space-between;align-items:baseline}.quality-control-list-component .header{display:inline-flex;justify-content:space-between;align-items:baseline}.raw-data-details-component .raw-data-details{display:flex;flex-direction:column;row-gap:var(--lumo-space-s)}.raw-data-details-component .raw-data-grid .sample-information{text-overflow:ellipsis;overflow:hidden;display:inline-flex;white-space:nowrap;width:80%}.raw-data-details-component .raw-data-grid>*{cursor:pointer}.raw-data-details-component .raw-data-grid ::part(row){cursor:pointer}.raw-data-details-component .raw-data-grid .raw-data-item{display:flex;flex-direction:column;row-gap:var(--lumo-space-s);cursor:default}.raw-data-details-component .raw-data-grid .raw-data-item .entry{display:inline-flex;column-gap:var(--lumo-space-s)}.raw-data-details-component .raw-data-grid .raw-data-item .entry .entry-label{font-weight:700;white-space:nowrap}.raw-data-details-component .raw-data-grid .raw-data-item .entry-value-list{display:flex;flex-wrap:wrap;gap:var(--lumo-space-s)}.raw-data-details-component .raw-data-grid .raw-data-item .entry-value{display:inline-flex;white-space:nowrap}.raw-data-download-information-component{display:flex;flex-direction:column;row-gap:var(--lumo-space-l)}.raw-data-download-information-component .section .section-title{display:inline-flex;align-items:center;column-gap:var(--lumo-space-m);font-weight:700}.project-details-component .ontology-entry-collection{display:inline-flex;flex-wrap:wrap;gap:var(--lumo-space-xs);width:100%;white-space:nowrap}.sample-details-component .button-and-search-bar{display:flex;justify-content:space-between;gap:var(--lumo-space-s);margin-bottom:var(--lumo-space-m)}.sample-details-component .button-bar{gap:var(--lumo-space-s);display:inline-flex;align-items:end}.sample-details-component .sample-details-content{display:flex;flex-direction:column;margin-bottom:var(--lumo-space-m);height:100%}.sample-details-component .sample-tab-content{height:100%;width:100%}.sample-details-component .search-bar{gap:var(--lumo-space-s);display:inline-flex;align-items:end}.user-profile-component{display:flex;flex-direction:column;row-gap:var(--lumo-space-xl)}.user-profile-component .user-details-card{display:flex;flex-direction:row;column-gap:5rem;background-color:var(--lumo-base-color);border:black;border-radius:var(--lumo-border-radius-m);box-shadow:var(--lumo-box-shadow-s);box-sizing:border-box;overflow:hidden;text-overflow:ellipsis;padding:5rem;min-width:fit-content;width:66%}.user-profile-component .change-name{color:var(--lumo-primary-text-color);cursor:pointer;width:fit-content}.user-profile-component .user-details-card .details{display:flex;flex-direction:column;row-gap:var(--lumo-space-l)}.user-profile-component .user-details-card .detail{display:flex;flex-direction:column;row-gap:var(--lumo-space-s)}.user-profile-component .user-details-card .avatar-with-name{display:flex;flex-direction:column;row-gap:var(--lumo-space-l);column-gap:initial}.user-profile-component .user-details-card .user-avatar{height:5rem;width:5rem}.code-block{display:inline-flex;border-radius:var(--lumo-border-radius-m);align-items:center;padding-inline:var(--lumo-space-s);justify-content:space-between;border:1px solid var(--lumo-contrast-20pct);column-gap:var(--lumo-space-l);font-family:Source Code Pro for Powerline,monospace}.experiment-list-item{display:flex;gap:var(--lumo-space-s);font-size:var(--lumo-font-size-l)}.experiment-list-item vaadin-icon{font-size:var(--lumo-font-size-m);padding:var(--lumo-space-xs)}.error-text{color:var(--lumo-error-text-color)}.info-box{display:inline-flex;border-radius:var(--lumo-border-radius-m);column-gap:var(--lumo-space-s);background-color:var(--lumo-contrast-5pct);align-items:center;width:fit-content;padding-inline:var(--lumo-space-m);padding-top:var(--lumo-space-s);padding-bottom:var(--lumo-space-s);font-size:small}.ontology-link{display:inline-flex;box-sizing:border-box;padding:.4em calc(.5em + var(--lumo-border-radius-s) / 4);color:var(--lumo-primary-text-color);background-color:var(--lumo-primary-color-10pct);border-radius:var(--lumo-border-radius-s);font-family:var(--lumo-font-family);font-size:var(--lumo-font-size-s);line-height:1;text-decoration-line:underline;width:min-content}span.bold{font-weight:700}span.clickable{cursor:pointer}span.primary{color:var(--lumo-primary-text-color)}span.secondary{color:var(--lumo-secondary-text-color);font-size:var(--lumo-font-size-s)}span.tertiary{color:var(--lumo-tertiary-text-color);font-size:var(--lumo-font-size-s)}span.warning{color:var(--lumo-warning-text-color);font-size:var(--lumo-font-size-s)}span.inline{display:inline-flex;column-gap:var(--lumo-space-xs)}.spreadsheet-list-item{display:inline-flex;width:100%;justify-content:space-between;align-items:center}.tag{display:inline-flex;align-items:center;justify-content:center;box-sizing:border-box;padding:.4em calc(.5em + var(--lumo-border-radius-s) / 4);border-radius:var(--lumo-border-radius-s);font-family:var(--lumo-font-family);font-size:var(--lumo-font-size-s);line-height:1;font-weight:500;text-transform:initial;letter-spacing:initial;min-width:calc(var(--lumo-line-height-xs) * 1em + .45em);flex-shrink:0}.tag:before{display:inline-block;content:" ";width:0}.tag.contrast{background-color:var(--lumo-contrast-5pct);color:var(--lumo-contrast-80pct)}.tag.error{background-color:var(--lumo-error-color-10pct);color:var(--lumo-error-color)}.tag.primary{background-color:var(--lumo-primary-color-10pct);color:var(--lumo-primary-text-color)}.tag.success{background-color:var(--lumo-success-color-10pct);color:var(--lumo-success-text-color)}.tag.violet{background-color:#7b61ff1a;color:#7b61ff}.tag.pink{background-color:#ff5dd226;color:#df0b92}.tag.warning{background-color:var(--lumo-warning-color-10pct);color:var(--lumo-warning-text-color)}.spreadsheet,.spreadsheet-container{width:100%;height:100%}h2{margin-bottom:.5em;margin-top:1.25em}vaadin-menu-bar-button{cursor:pointer}vaadin-menu-bar-item{cursor:pointer}vaadin-form-item::part(content){cursor:pointer}vaadin-multi-select-combo-box.chip-badge vaadin-multi-select-combo-box-chip{color:var(--lumo-primary-text-color);background-color:var(--lumo-primary-color-10pct);font-size:var(--lumo-font-size-s)}vaadin-multi-select-combo-box-item{align-items:start}vaadin-multi-select-combo-box.chip-badge vaadin-multi-select-combo-box-chip[slot=overflow]:before,vaadin-multi-select-combo-box-chip[slot=overflow]:after{border-color:var(--lumo-primary-color-10pct)}vaadin-multi-select-combo-box::part(toggle-button):before{color:var(--lumo-primary-color)}vaadin-multi-select-combo-box.no-chevron::part(toggle-button){display:none}vaadin-number-field::part(decrease-button),vaadin-number-field::part(increase-button){color:var(--lumo-primary-color)}vaadin-tabsheet.minimal::part(tabs-container){box-shadow:none;--_lumo-tab-marker-display: none}vaadin-list-box.transparent-icons vaadin-item::part(checkmark):before{display:none}vaadin-menu-bar-item.transparent-icon::part(checkmark):before{color:transparent}.measurement-template-list-component .measurement-template-list{height:fit-content}.measurement-template-list-component .header{display:flex;justify-content:space-between;align-items:center;gap:var(--lumo-space-m)}.measurement-template-list-component .measurement-template-list-item{display:inline-flex;align-items:flex-start;justify-content:space-between;width:100%;column-gap:var(--lumo-space-l);padding-top:var(--lumo-space-s);padding-bottom:var(--lumo-space-s)}.measurement-template-list-component .measurement-template-list-item .controls{display:inline-flex;align-items:end;justify-items:center;column-gap:var(--lumo-space-s);padding-inline:var(--lumo-space-m)}.measurement-template-list-component .measurement-template-list-item .file-name{overflow:hidden;text-overflow:ellipsis}.measurement-template-list-component .measurement-template-list-item .file-icon{display:inline-flex;font-size:smaller;flex-shrink:0;color:var(--lumo-tertiary-text-color)}.measurement-template-list-component .measurement-template-list-item .file-info{display:inline-flex;flex-direction:column;gap:var(--lumo-space-s);overflow:hidden}.measurement-template-list-component .measurement-template-list-item .file-info-with-icon{display:inline-flex;gap:var(--lumo-space-s);overflow:hidden;align-items:center}.offer-list-component .header{display:inline-flex;justify-content:space-between;align-items:baseline}.offer-list-component .offer-info{display:inline-flex;align-items:center;justify-content:space-between;width:100%;column-gap:var(--lumo-space-l);padding-top:var(--lumo-space-s);padding-bottom:var(--lumo-space-s)}.offer-list-component .offer-info .controls{display:inline-flex;align-items:end;justify-items:center;column-gap:var(--lumo-space-s);padding-inline:var(--lumo-space-m)}.offer-list-component .offer-info .file-icon{display:inline-flex;font-size:smaller;flex-shrink:0;color:var(--lumo-tertiary-text-color)}.offer-list-component .offer-info .file-info{display:inline-flex;gap:var(--lumo-space-s);align-items:center;overflow:hidden}.offer-list-component .offer-info .file-name{overflow:hidden;text-overflow:ellipsis}.offer-list-component .offer-list{height:fit-content}.offer-list-component .offer-list .header{display:flex;justify-content:space-between;align-items:center;gap:var(--lumo-space-m)}.offer-list-component .offer-list .offer-list-item{display:inline-flex;justify-content:space-between}.offer-list-component .offer-info .signed-info{width:var(--lumo-icon-size-s);flex-shrink:0;justify-self:right}.offer-list-component .offer-info .signed-info.signed{color:var(--lumo-primary-color)}.offer-list-component .offer-info .signed-info.unsigned{color:var(--lumo-tertiary-text-color)}.quality-control-list-component .quality-control-list{height:fit-content}.quality-control-list-component .header{display:flex;justify-content:space-between;align-items:center;gap:var(--lumo-space-m)}.quality-control-list-component .quality-control-item{display:inline-flex;align-items:flex-start;justify-content:space-between;width:100%;column-gap:var(--lumo-space-l);padding-top:var(--lumo-space-s);padding-bottom:var(--lumo-space-s)}.quality-control-list-component .quality-control-item .controls{display:inline-flex;align-items:end;justify-items:center;column-gap:var(--lumo-space-s);padding-inline:var(--lumo-space-m)}.quality-control-list-component .quality-control-item .file-name{overflow:hidden;text-overflow:ellipsis}.quality-control-list-component .quality-control-item .file-icon{display:inline-flex;font-size:smaller;flex-shrink:0;color:var(--lumo-tertiary-text-color)}.quality-control-list-component .quality-control-item .file-info{display:inline-flex;flex-direction:column;gap:var(--lumo-space-s);overflow:hidden}.quality-control-list-component .quality-control-item .file-info-with-icon{display:inline-flex;gap:var(--lumo-space-s);overflow:hidden}vaadin-avatar-group vaadin-avatar,.user-avatar{background-color:#fff;border-color:var(--lumo-contrast-20pct)}.avatar-with-name{display:inline-flex;justify-content:center;align-items:center;flex-direction:row;column-gap:var(--lumo-space-s);row-gap:initial}
`,fl=`.analysis-type-combo-box::part(overlay){width:20rem}
`,vl=`.card{background-color:var(--lumo-base-color);border:black;border-radius:var(--lumo-border-radius-m);box-shadow:var(--lumo-box-shadow-s);box-sizing:border-box;overflow:hidden;text-overflow:ellipsis;margin:4px}.creation-card{display:flex;justify-content:center;padding:1.5rem;flex:auto;max-width:300px}.creation-card>div{display:flex;flex-direction:column;justify-content:center;align-items:center;cursor:pointer}.creation-card .disclaimer-area{font-size:smaller}.creation-card .button{background-color:var(--lumo-button-primary-background-color, var(--lumo-primary-color));color:var(--lumo-button-primary-color, var(--lumo-primary-contrast-color));font-weight:600;min-width:calc(var(--lumo-button-size) * 2.5)}.creation-card .button:before{background-color:#000}.creation-card .button:hover:before{opacity:.05}.experimental-group{display:flex;flex-direction:column;flex:auto;font-size:var(--lumo-font-size-s);padding:var(--lumo-space-l);max-width:300px}.card-collection{display:flex;align-content:space-evenly;flex-flow:column;gap:1rem;margin-left:1.5rem;margin-top:1.5rem}.card-collection .collection-title{font-weight:700;color:var(--lumo-secondary-text-color);font-size:var(--lumo-font-size-m);margin-bottom:.5rem}.card-collection .collection-header{justify-content:space-between;display:flex}.card-collection .collection-content{display:flex;align-content:space-evenly;flex-flow:row wrap;gap:1rem;flex-direction:row}.card-collection .collection-controls{display:flex;align-content:space-evenly;flex-flow:row wrap;gap:.5rem}.experimental-group .header{display:flex;justify-content:space-between;align-items:baseline;margin-bottom:var(--lumo-space-m)}.experimental-group .card-title{color:var(--lumo-secondary-text-color);font-size:var(--lumo-font-size-m);font-weight:700;white-space:nowrap;margin-bottom:.5rem}.experimental-group .content{display:inline-flex;flex-wrap:wrap;gap:var(--lumo-space-m);margin-bottom:var(--lumo-space-m)}.experimental-group vaadin-icon{cursor:pointer;width:1em;color:#a9a9a9}.card.experiment-info{flex:auto;font-size:var(--lumo-font-size-s);padding:var(--lumo-space-l);max-width:300px}.card.experiment-info .title{font-size:var(--lumo-font-size-l);margin-bottom:.5rem;font-weight:700;color:var(--lumo-secondary-text-color)}.card.experiment-info .content{display:flex;flex-direction:column;justify-content:center;align-items:stretch}.experiment-info ul{list-style-type:none;display:block}.experiment-info .header{color:var(--lumo-header-text-color);font-size:var(--lumo-font-size-m);font-weight:700;display:flex;justify-content:space-between;align-items:baseline}.experiment-info .title{font-weight:700}.experiment-item{display:block;min-height:100px;width:100%}.experiment-item:hover{background-color:#f5f5f5;cursor:pointer}.experiment-item .experiment-title{font-weight:700;padding-left:1rem;color:#3f3f3f}.experiment-item .content-section{padding:1rem 1rem 0 0;display:flex;justify-content:space-between}.experiment-item .progress-section{background-color:#dcdcdc;font-size:smaller;padding-left:1rem}.experiment-item .progress-section .incomplete{background-color:#dcdcdc}.experiment-item vaadin-icon{color:#949494;min-width:var(--lumo-icon-size-m);min-height:var(--lumo-icon-size-m)}.experiment-item .active-section{padding-left:1rem;padding-bottom:var(--lumo-space-s);padding-top:var(--lumo-space-l)}.experiment-item.selected{background-color:#fff;margin-left:1rem}.experiment-item.selected .content-section{padding:1rem 2rem 0 0}.experiment-item.selected vaadin-icon{color:var(--lumo-primary-color)}
`,xl=`.navigation-button{display:flex;flex-direction:column;gap:var(--lumo-space-m);align-items:center;flex:1 1}.navigation-button .button{box-shadow:var(--lumo-box-shadow-xs)}.navigation-button .label{font-size:var(--lumo-font-size-s)}vaadin-button.primary{background-color:var(--lumo-button-primary-background-color, var(--lumo-primary-color));color:var(--lumo-button-primary-color, var(--lumo-primary-contrast-color));font-weight:600;min-width:calc(var(--lumo-button-size) * 2.5)}vaadin-button[disabled]{background-color:var(--lumo-contrast-30pct);color:var(--lumo-base-color)}vaadin-button.primary .button:before{background-color:#000}vaadin-button.primary .button:hover:before{opacity:.05}vaadin-button.tertiary{padding:0 calc(var(--lumo-button-size) / 6);background-color:transparent!important;min-width:0}vaadin-button{cursor:pointer}vaadin-button[theme~=tertiary] vaadin-icon,vaadin-button[theme~=tertiary-inline] vaadin-icon{color:var(--lumo-tertiary-text-color)}
`;document._vaadintheme_datamanager_componentCss||(T("virtuallist",A(ol.toString())),T("vaadin-custom",A(il.toString())),T("spreadsheet",A(al.toString())),T("span",A(nl.toString())),T("page-area",A(rl.toString())),T("navigation-drawer",A(sl.toString())),T("navbar",A(ll.toString())),T("main",A(cl.toString())),T("info",A(dl.toString())),T("image",A(ml.toString())),T("icon",A(pl.toString())),T("div",A(ul.toString())),T("dialog",A(hl.toString())),T("custom",A(gl.toString())),T("combobox",A(fl.toString())),T("card",A(vl.toString())),T("button",A(xl.toString())),document._vaadintheme_datamanager_componentCss=!0);/**
 * @license
 * Copyright (c) 2021 - 2023 Vaadin Ltd.
 * This program is available under Apache License Version 2.0, available at https://vaadin.com/license/
 */function yl(t){const e=customElements.get(t.is);if(!e)customElements.define(t.is,t);else{const o=e.version;o&&t.version&&o===t.version?console.warn(`The component ${t.is} has been loaded twice`):console.error(`Tried to define ${t.is} version ${t.version} when version ${e.version} is already in use. Something will probably break.`)}}/**
 * @license
 * Copyright (c) 2017 - 2023 Vaadin Ltd.
 * This program is available under Apache License Version 2.0, available at https://vaadin.com/license/
 */class bl extends HTMLElement{static get is(){return"vaadin-lumo-styles"}static get version(){return"24.2.7"}}yl(bl);const ye=(t,...e)=>{tl(`lumo-${t}`,e)};/**
 * @license
 * Copyright (c) 2017 - 2023 Vaadin Ltd.
 * This program is available under Apache License Version 2.0, available at https://vaadin.com/license/
 */const wl=k`
  :host {
    /* prettier-ignore */
    --lumo-font-family: -apple-system, BlinkMacSystemFont, 'Roboto', 'Segoe UI', Helvetica, Arial, sans-serif, 'Apple Color Emoji', 'Segoe UI Emoji', 'Segoe UI Symbol';

    /* Font sizes */
    --lumo-font-size-xxs: 0.75rem;
    --lumo-font-size-xs: 0.8125rem;
    --lumo-font-size-s: 0.875rem;
    --lumo-font-size-m: 1rem;
    --lumo-font-size-l: 1.125rem;
    --lumo-font-size-xl: 1.375rem;
    --lumo-font-size-xxl: 1.75rem;
    --lumo-font-size-xxxl: 2.5rem;

    /* Line heights */
    --lumo-line-height-xs: 1.25;
    --lumo-line-height-s: 1.375;
    --lumo-line-height-m: 1.625;
  }
`,Ko=k`
  body,
  :host {
    font-family: var(--lumo-font-family);
    font-size: var(--lumo-font-size-m);
    line-height: var(--lumo-line-height-m);
    -webkit-text-size-adjust: 100%;
    -webkit-font-smoothing: antialiased;
    -moz-osx-font-smoothing: grayscale;
  }

  small,
  [theme~='font-size-s'] {
    font-size: var(--lumo-font-size-s);
    line-height: var(--lumo-line-height-s);
  }

  [theme~='font-size-xs'] {
    font-size: var(--lumo-font-size-xs);
    line-height: var(--lumo-line-height-xs);
  }

  :where(h1, h2, h3, h4, h5, h6) {
    font-weight: 600;
    line-height: var(--lumo-line-height-xs);
    margin-block: 0;
  }

  :where(h1) {
    font-size: var(--lumo-font-size-xxxl);
  }

  :where(h2) {
    font-size: var(--lumo-font-size-xxl);
  }

  :where(h3) {
    font-size: var(--lumo-font-size-xl);
  }

  :where(h4) {
    font-size: var(--lumo-font-size-l);
  }

  :where(h5) {
    font-size: var(--lumo-font-size-m);
  }

  :where(h6) {
    font-size: var(--lumo-font-size-xs);
    text-transform: uppercase;
    letter-spacing: 0.03em;
  }

  p,
  blockquote {
    margin-top: 0.5em;
    margin-bottom: 0.75em;
  }

  a {
    text-decoration: none;
  }

  a:where(:any-link):hover {
    text-decoration: underline;
  }

  hr {
    display: block;
    align-self: stretch;
    height: 1px;
    border: 0;
    padding: 0;
    margin: var(--lumo-space-s) calc(var(--lumo-border-radius-m) / 2);
    background-color: var(--lumo-contrast-10pct);
  }

  blockquote {
    border-left: 2px solid var(--lumo-contrast-30pct);
  }

  b,
  strong {
    font-weight: 600;
  }

  /* RTL specific styles */
  blockquote[dir='rtl'] {
    border-left: none;
    border-right: 2px solid var(--lumo-contrast-30pct);
  }
`;T("",Ko,{moduleId:"lumo-typography"});ye("typography-props",wl);/**
 * @license
 * Copyright (c) 2017 - 2023 Vaadin Ltd.
 * This program is available under Apache License Version 2.0, available at https://vaadin.com/license/
 */const _l=k`
  ${A(Ko.cssText.replace(/,\s*:host/su,""))}
`;ye("typography",_l,!1);/**
 * @license
 * Copyright (c) 2017 - 2023 Vaadin Ltd.
 * This program is available under Apache License Version 2.0, available at https://vaadin.com/license/
 */const kl=k`
  :host {
    /* Base (background) */
    --lumo-base-color: #fff;

    /* Tint */
    --lumo-tint-5pct: hsla(0, 0%, 100%, 0.3);
    --lumo-tint-10pct: hsla(0, 0%, 100%, 0.37);
    --lumo-tint-20pct: hsla(0, 0%, 100%, 0.44);
    --lumo-tint-30pct: hsla(0, 0%, 100%, 0.5);
    --lumo-tint-40pct: hsla(0, 0%, 100%, 0.57);
    --lumo-tint-50pct: hsla(0, 0%, 100%, 0.64);
    --lumo-tint-60pct: hsla(0, 0%, 100%, 0.7);
    --lumo-tint-70pct: hsla(0, 0%, 100%, 0.77);
    --lumo-tint-80pct: hsla(0, 0%, 100%, 0.84);
    --lumo-tint-90pct: hsla(0, 0%, 100%, 0.9);
    --lumo-tint: #fff;

    /* Shade */
    --lumo-shade-5pct: hsla(214, 61%, 25%, 0.05);
    --lumo-shade-10pct: hsla(214, 57%, 24%, 0.1);
    --lumo-shade-20pct: hsla(214, 53%, 23%, 0.16);
    --lumo-shade-30pct: hsla(214, 50%, 22%, 0.26);
    --lumo-shade-40pct: hsla(214, 47%, 21%, 0.38);
    --lumo-shade-50pct: hsla(214, 45%, 20%, 0.52);
    --lumo-shade-60pct: hsla(214, 43%, 19%, 0.6);
    --lumo-shade-70pct: hsla(214, 42%, 18%, 0.69);
    --lumo-shade-80pct: hsla(214, 41%, 17%, 0.83);
    --lumo-shade-90pct: hsla(214, 40%, 16%, 0.94);
    --lumo-shade: hsl(214, 35%, 15%);

    /* Contrast */
    --lumo-contrast-5pct: var(--lumo-shade-5pct);
    --lumo-contrast-10pct: var(--lumo-shade-10pct);
    --lumo-contrast-20pct: var(--lumo-shade-20pct);
    --lumo-contrast-30pct: var(--lumo-shade-30pct);
    --lumo-contrast-40pct: var(--lumo-shade-40pct);
    --lumo-contrast-50pct: var(--lumo-shade-50pct);
    --lumo-contrast-60pct: var(--lumo-shade-60pct);
    --lumo-contrast-70pct: var(--lumo-shade-70pct);
    --lumo-contrast-80pct: var(--lumo-shade-80pct);
    --lumo-contrast-90pct: var(--lumo-shade-90pct);
    --lumo-contrast: var(--lumo-shade);

    /* Text */
    --lumo-header-text-color: var(--lumo-contrast);
    --lumo-body-text-color: var(--lumo-contrast-90pct);
    --lumo-secondary-text-color: var(--lumo-contrast-70pct);
    --lumo-tertiary-text-color: var(--lumo-contrast-50pct);
    --lumo-disabled-text-color: var(--lumo-contrast-30pct);

    /* Primary */
    --lumo-primary-color: hsl(214, 100%, 48%);
    --lumo-primary-color-50pct: hsla(214, 100%, 49%, 0.76);
    --lumo-primary-color-10pct: hsla(214, 100%, 60%, 0.13);
    --lumo-primary-text-color: hsl(214, 100%, 43%);
    --lumo-primary-contrast-color: #fff;

    /* Error */
    --lumo-error-color: hsl(3, 85%, 48%);
    --lumo-error-color-50pct: hsla(3, 85%, 49%, 0.5);
    --lumo-error-color-10pct: hsla(3, 85%, 49%, 0.1);
    --lumo-error-text-color: hsl(3, 89%, 42%);
    --lumo-error-contrast-color: #fff;

    /* Success */
    --lumo-success-color: hsl(145, 72%, 30%);
    --lumo-success-color-50pct: hsla(145, 72%, 31%, 0.5);
    --lumo-success-color-10pct: hsla(145, 72%, 31%, 0.1);
    --lumo-success-text-color: hsl(145, 85%, 25%);
    --lumo-success-contrast-color: #fff;

    /* Warning */
    --lumo-warning-color: hsl(48, 100%, 50%);
    --lumo-warning-color-10pct: hsla(48, 100%, 50%, 0.25);
    --lumo-warning-text-color: hsl(32, 100%, 30%);
    --lumo-warning-contrast-color: var(--lumo-shade-90pct);
  }

  /* forced-colors mode adjustments */
  @media (forced-colors: active) {
    html {
      --lumo-disabled-text-color: GrayText;
    }
  }
`;ye("color-props",kl);const Yo=k`
  [theme~='dark'] {
    /* Base (background) */
    --lumo-base-color: hsl(214, 35%, 21%);

    /* Tint */
    --lumo-tint-5pct: hsla(214, 65%, 85%, 0.06);
    --lumo-tint-10pct: hsla(214, 60%, 80%, 0.14);
    --lumo-tint-20pct: hsla(214, 64%, 82%, 0.23);
    --lumo-tint-30pct: hsla(214, 69%, 84%, 0.32);
    --lumo-tint-40pct: hsla(214, 73%, 86%, 0.41);
    --lumo-tint-50pct: hsla(214, 78%, 88%, 0.5);
    --lumo-tint-60pct: hsla(214, 82%, 90%, 0.58);
    --lumo-tint-70pct: hsla(214, 87%, 92%, 0.69);
    --lumo-tint-80pct: hsla(214, 91%, 94%, 0.8);
    --lumo-tint-90pct: hsla(214, 96%, 96%, 0.9);
    --lumo-tint: hsl(214, 100%, 98%);

    /* Shade */
    --lumo-shade-5pct: hsla(214, 0%, 0%, 0.07);
    --lumo-shade-10pct: hsla(214, 4%, 2%, 0.15);
    --lumo-shade-20pct: hsla(214, 8%, 4%, 0.23);
    --lumo-shade-30pct: hsla(214, 12%, 6%, 0.32);
    --lumo-shade-40pct: hsla(214, 16%, 8%, 0.41);
    --lumo-shade-50pct: hsla(214, 20%, 10%, 0.5);
    --lumo-shade-60pct: hsla(214, 24%, 12%, 0.6);
    --lumo-shade-70pct: hsla(214, 28%, 13%, 0.7);
    --lumo-shade-80pct: hsla(214, 32%, 13%, 0.8);
    --lumo-shade-90pct: hsla(214, 33%, 13%, 0.9);
    --lumo-shade: hsl(214, 33%, 13%);

    /* Contrast */
    --lumo-contrast-5pct: var(--lumo-tint-5pct);
    --lumo-contrast-10pct: var(--lumo-tint-10pct);
    --lumo-contrast-20pct: var(--lumo-tint-20pct);
    --lumo-contrast-30pct: var(--lumo-tint-30pct);
    --lumo-contrast-40pct: var(--lumo-tint-40pct);
    --lumo-contrast-50pct: var(--lumo-tint-50pct);
    --lumo-contrast-60pct: var(--lumo-tint-60pct);
    --lumo-contrast-70pct: var(--lumo-tint-70pct);
    --lumo-contrast-80pct: var(--lumo-tint-80pct);
    --lumo-contrast-90pct: var(--lumo-tint-90pct);
    --lumo-contrast: var(--lumo-tint);

    /* Text */
    --lumo-header-text-color: var(--lumo-contrast);
    --lumo-body-text-color: var(--lumo-contrast-90pct);
    --lumo-secondary-text-color: var(--lumo-contrast-70pct);
    --lumo-tertiary-text-color: var(--lumo-contrast-50pct);
    --lumo-disabled-text-color: var(--lumo-contrast-30pct);

    /* Primary */
    --lumo-primary-color: hsl(214, 90%, 48%);
    --lumo-primary-color-50pct: hsla(214, 90%, 70%, 0.69);
    --lumo-primary-color-10pct: hsla(214, 90%, 55%, 0.13);
    --lumo-primary-text-color: hsl(214, 90%, 77%);
    --lumo-primary-contrast-color: #fff;

    /* Error */
    --lumo-error-color: hsl(3, 79%, 49%);
    --lumo-error-color-50pct: hsla(3, 75%, 62%, 0.5);
    --lumo-error-color-10pct: hsla(3, 75%, 62%, 0.14);
    --lumo-error-text-color: hsl(3, 100%, 80%);

    /* Success */
    --lumo-success-color: hsl(145, 72%, 30%);
    --lumo-success-color-50pct: hsla(145, 92%, 51%, 0.5);
    --lumo-success-color-10pct: hsla(145, 92%, 51%, 0.1);
    --lumo-success-text-color: hsl(145, 85%, 46%);

    /* Warning */
    --lumo-warning-color: hsl(43, 100%, 48%);
    --lumo-warning-color-10pct: hsla(40, 100%, 50%, 0.2);
    --lumo-warning-text-color: hsl(45, 100%, 60%);
    --lumo-warning-contrast-color: var(--lumo-shade-90pct);
  }

  html {
    color: var(--lumo-body-text-color);
    background-color: var(--lumo-base-color);
    color-scheme: light;
  }

  [theme~='dark'] {
    color: var(--lumo-body-text-color);
    background-color: var(--lumo-base-color);
    color-scheme: dark;
  }

  h1,
  h2,
  h3,
  h4,
  h5,
  h6 {
    color: var(--lumo-header-text-color);
  }

  a:where(:any-link) {
    color: var(--lumo-primary-text-color);
  }

  a:not(:any-link) {
    color: var(--lumo-disabled-text-color);
  }

  blockquote {
    color: var(--lumo-secondary-text-color);
  }

  code,
  pre {
    background-color: var(--lumo-contrast-10pct);
    border-radius: var(--lumo-border-radius-m);
  }
`;T("",Yo,{moduleId:"lumo-color"});/**
 * @license
 * Copyright (c) 2017 - 2023 Vaadin Ltd.
 * This program is available under Apache License Version 2.0, available at https://vaadin.com/license/
 */ye("color",Yo);/**
 * @license
 * Copyright (c) 2017 - 2023 Vaadin Ltd.
 * This program is available under Apache License Version 2.0, available at https://vaadin.com/license/
 */const Pa=k`
  :host {
    /* Square */
    --lumo-space-xs: 0.25rem;
    --lumo-space-s: 0.5rem;
    --lumo-space-m: 1rem;
    --lumo-space-l: 1.5rem;
    --lumo-space-xl: 2.5rem;

    /* Wide */
    --lumo-space-wide-xs: calc(var(--lumo-space-xs) / 2) var(--lumo-space-xs);
    --lumo-space-wide-s: calc(var(--lumo-space-s) / 2) var(--lumo-space-s);
    --lumo-space-wide-m: calc(var(--lumo-space-m) / 2) var(--lumo-space-m);
    --lumo-space-wide-l: calc(var(--lumo-space-l) / 2) var(--lumo-space-l);
    --lumo-space-wide-xl: calc(var(--lumo-space-xl) / 2) var(--lumo-space-xl);

    /* Tall */
    --lumo-space-tall-xs: var(--lumo-space-xs) calc(var(--lumo-space-xs) / 2);
    --lumo-space-tall-s: var(--lumo-space-s) calc(var(--lumo-space-s) / 2);
    --lumo-space-tall-m: var(--lumo-space-m) calc(var(--lumo-space-m) / 2);
    --lumo-space-tall-l: var(--lumo-space-l) calc(var(--lumo-space-l) / 2);
    --lumo-space-tall-xl: var(--lumo-space-xl) calc(var(--lumo-space-xl) / 2);
  }
`;ye("spacing-props",Pa);/**
 * @license
 * Copyright (c) 2017 - 2023 Vaadin Ltd.
 * This program is available under Apache License Version 2.0, available at https://vaadin.com/license/
 */const El=k`
  :host {
    /* Border radius */
    --lumo-border-radius-s: 0.25em; /* Checkbox, badge, date-picker year indicator, etc */
    --lumo-border-radius-m: var(--lumo-border-radius, 0.25em); /* Button, text field, menu overlay, etc */
    --lumo-border-radius-l: 0.5em; /* Dialog, notification, etc */

    /* Shadow */
    --lumo-box-shadow-xs: 0 1px 4px -1px var(--lumo-shade-50pct);
    --lumo-box-shadow-s: 0 2px 4px -1px var(--lumo-shade-20pct), 0 3px 12px -1px var(--lumo-shade-30pct);
    --lumo-box-shadow-m: 0 2px 6px -1px var(--lumo-shade-20pct), 0 8px 24px -4px var(--lumo-shade-40pct);
    --lumo-box-shadow-l: 0 3px 18px -2px var(--lumo-shade-20pct), 0 12px 48px -6px var(--lumo-shade-40pct);
    --lumo-box-shadow-xl: 0 4px 24px -3px var(--lumo-shade-20pct), 0 18px 64px -8px var(--lumo-shade-40pct);

    /* Clickable element cursor */
    --lumo-clickable-cursor: default;
  }
`;k`
  html {
    --vaadin-checkbox-size: calc(var(--lumo-size-m) / 2);
    --vaadin-radio-button-size: calc(var(--lumo-size-m) / 2);
    --vaadin-input-field-border-radius: var(--lumo-border-radius-m);
  }
`;ye("style-props",El);/**
 * @license
 * Copyright (c) 2017 - 2023 Vaadin Ltd.
 * This program is available under Apache License Version 2.0, available at https://vaadin.com/license/
 */const Jo=k`
  [theme~='badge'] {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    box-sizing: border-box;
    padding: 0.4em calc(0.5em + var(--lumo-border-radius-s) / 4);
    color: var(--lumo-primary-text-color);
    background-color: var(--lumo-primary-color-10pct);
    border-radius: var(--lumo-border-radius-s);
    font-family: var(--lumo-font-family);
    font-size: var(--lumo-font-size-s);
    line-height: 1;
    font-weight: 500;
    text-transform: initial;
    letter-spacing: initial;
    min-width: calc(var(--lumo-line-height-xs) * 1em + 0.45em);
    flex-shrink: 0;
  }

  /* Ensure proper vertical alignment */
  [theme~='badge']::before {
    display: inline-block;
    content: '\\2003';
    width: 0;
  }

  [theme~='badge'][theme~='small'] {
    font-size: var(--lumo-font-size-xxs);
    line-height: 1;
  }

  /* Colors */

  [theme~='badge'][theme~='success'] {
    color: var(--lumo-success-text-color);
    background-color: var(--lumo-success-color-10pct);
  }

  [theme~='badge'][theme~='error'] {
    color: var(--lumo-error-text-color);
    background-color: var(--lumo-error-color-10pct);
  }

  [theme~='badge'][theme~='warning'] {
    color: var(--lumo-warning-text-color);
    background-color: var(--lumo-warning-color-10pct);
  }

  [theme~='badge'][theme~='contrast'] {
    color: var(--lumo-contrast-80pct);
    background-color: var(--lumo-contrast-5pct);
  }

  /* Primary */

  [theme~='badge'][theme~='primary'] {
    color: var(--lumo-primary-contrast-color);
    background-color: var(--lumo-primary-color);
  }

  [theme~='badge'][theme~='success'][theme~='primary'] {
    color: var(--lumo-success-contrast-color);
    background-color: var(--lumo-success-color);
  }

  [theme~='badge'][theme~='error'][theme~='primary'] {
    color: var(--lumo-error-contrast-color);
    background-color: var(--lumo-error-color);
  }

  [theme~='badge'][theme~='warning'][theme~='primary'] {
    color: var(--lumo-warning-contrast-color);
    background-color: var(--lumo-warning-color);
  }

  [theme~='badge'][theme~='contrast'][theme~='primary'] {
    color: var(--lumo-base-color);
    background-color: var(--lumo-contrast);
  }

  /* Links */

  [theme~='badge'][href]:hover {
    text-decoration: none;
  }

  /* Icon */

  [theme~='badge'] vaadin-icon {
    margin: -0.25em 0;
  }

  [theme~='badge'] vaadin-icon:first-child {
    margin-left: -0.375em;
  }

  [theme~='badge'] vaadin-icon:last-child {
    margin-right: -0.375em;
  }

  vaadin-icon[theme~='badge'][icon] {
    min-width: 0;
    padding: 0;
    font-size: 1rem;
    width: var(--lumo-icon-size-m);
    height: var(--lumo-icon-size-m);
  }

  vaadin-icon[theme~='badge'][icon][theme~='small'] {
    width: var(--lumo-icon-size-s);
    height: var(--lumo-icon-size-s);
  }

  /* Empty */

  [theme~='badge']:not([icon]):empty {
    min-width: 0;
    width: 1em;
    height: 1em;
    padding: 0;
    border-radius: 50%;
    background-color: var(--lumo-primary-color);
  }

  [theme~='badge'][theme~='small']:not([icon]):empty {
    width: 0.75em;
    height: 0.75em;
  }

  [theme~='badge'][theme~='contrast']:not([icon]):empty {
    background-color: var(--lumo-contrast);
  }

  [theme~='badge'][theme~='success']:not([icon]):empty {
    background-color: var(--lumo-success-color);
  }

  [theme~='badge'][theme~='error']:not([icon]):empty {
    background-color: var(--lumo-error-color);
  }

  [theme~='badge'][theme~='warning']:not([icon]):empty {
    background-color: var(--lumo-warning-color);
  }

  /* Pill */

  [theme~='badge'][theme~='pill'] {
    --lumo-border-radius-s: 1em;
  }

  /* RTL specific styles */

  [dir='rtl'][theme~='badge'] vaadin-icon:first-child {
    margin-right: -0.375em;
    margin-left: 0;
  }

  [dir='rtl'][theme~='badge'] vaadin-icon:last-child {
    margin-left: -0.375em;
    margin-right: 0;
  }
`;T("",Jo,{moduleId:"lumo-badge"});/**
 * @license
 * Copyright (c) 2017 - 2023 Vaadin Ltd.
 * This program is available under Apache License Version 2.0, available at https://vaadin.com/license/
 */ye("badge",Jo);/**
 * @license
 * Copyright (c) 2017 - 2023 Vaadin Ltd.
 * This program is available under Apache License Version 2.0, available at https://vaadin.com/license/
 */const Sl=k`
  /* === Screen readers === */
  .sr-only {
    border-width: 0;
    clip: rect(0, 0, 0, 0);
    height: 1px;
    margin: -1px;
    overflow: hidden;
    padding: 0;
    position: absolute;
    white-space: nowrap;
    width: 1px;
  }
`;/**
 * @license
 * Copyright (c) 2017 - 2023 Vaadin Ltd.
 * This program is available under Apache License Version 2.0, available at https://vaadin.com/license/
 */const Cl=k`
  /* === Background color === */
  .bg-base {
    background-color: var(--lumo-base-color);
  }

  .bg-transparent {
    background-color: transparent;
  }

  .bg-contrast-5 {
    background-color: var(--lumo-contrast-5pct);
  }
  .bg-contrast-10 {
    background-color: var(--lumo-contrast-10pct);
  }
  .bg-contrast-20 {
    background-color: var(--lumo-contrast-20pct);
  }
  .bg-contrast-30 {
    background-color: var(--lumo-contrast-30pct);
  }
  .bg-contrast-40 {
    background-color: var(--lumo-contrast-40pct);
  }
  .bg-contrast-50 {
    background-color: var(--lumo-contrast-50pct);
  }
  .bg-contrast-60 {
    background-color: var(--lumo-contrast-60pct);
  }
  .bg-contrast-70 {
    background-color: var(--lumo-contrast-70pct);
  }
  .bg-contrast-80 {
    background-color: var(--lumo-contrast-80pct);
  }
  .bg-contrast-90 {
    background-color: var(--lumo-contrast-90pct);
  }
  .bg-contrast {
    background-color: var(--lumo-contrast);
  }

  .bg-primary {
    background-color: var(--lumo-primary-color);
  }
  .bg-primary-50 {
    background-color: var(--lumo-primary-color-50pct);
  }
  .bg-primary-10 {
    background-color: var(--lumo-primary-color-10pct);
  }

  .bg-error {
    background-color: var(--lumo-error-color);
  }
  .bg-error-50 {
    background-color: var(--lumo-error-color-50pct);
  }
  .bg-error-10 {
    background-color: var(--lumo-error-color-10pct);
  }

  .bg-success {
    background-color: var(--lumo-success-color);
  }
  .bg-success-50 {
    background-color: var(--lumo-success-color-50pct);
  }
  .bg-success-10 {
    background-color: var(--lumo-success-color-10pct);
  }

  .bg-warning {
    background-color: var(--lumo-warning-color);
  }
  .bg-warning-10 {
    background-color: var(--lumo-warning-color-10pct);
  }
`;/**
 * @license
 * Copyright (c) 2017 - 2023 Vaadin Ltd.
 * This program is available under Apache License Version 2.0, available at https://vaadin.com/license/
 */const zl=k`
  /* === Border === */
  .border-0 {
    border: none;
  }
  .border {
    border: 1px solid;
  }
  .border-b {
    border-bottom: 1px solid;
  }
  .border-l {
    border-left: 1px solid;
  }
  .border-r {
    border-right: 1px solid;
  }
  .border-t {
    border-top: 1px solid;
  }

  /* === Border color === */
  .border-contrast-5 {
    border-color: var(--lumo-contrast-5pct);
  }
  .border-contrast-10 {
    border-color: var(--lumo-contrast-10pct);
  }
  .border-contrast-20 {
    border-color: var(--lumo-contrast-20pct);
  }
  .border-contrast-30 {
    border-color: var(--lumo-contrast-30pct);
  }
  .border-contrast-40 {
    border-color: var(--lumo-contrast-40pct);
  }
  .border-contrast-50 {
    border-color: var(--lumo-contrast-50pct);
  }
  .border-contrast-60 {
    border-color: var(--lumo-contrast-60pct);
  }
  .border-contrast-70 {
    border-color: var(--lumo-contrast-70pct);
  }
  .border-contrast-80 {
    border-color: var(--lumo-contrast-80pct);
  }
  .border-contrast-90 {
    border-color: var(--lumo-contrast-90pct);
  }
  .border-contrast {
    border-color: var(--lumo-contrast);
  }

  .border-primary {
    border-color: var(--lumo-primary-color);
  }
  .border-primary-50 {
    border-color: var(--lumo-primary-color-50pct);
  }
  .border-primary-10 {
    border-color: var(--lumo-primary-color-10pct);
  }

  .border-error {
    border-color: var(--lumo-error-color);
  }
  .border-error-50 {
    border-color: var(--lumo-error-color-50pct);
  }
  .border-error-10 {
    border-color: var(--lumo-error-color-10pct);
  }

  .border-success {
    border-color: var(--lumo-success-color);
  }
  .border-success-50 {
    border-color: var(--lumo-success-color-50pct);
  }
  .border-success-10 {
    border-color: var(--lumo-success-color-10pct);
  }

  .border-warning {
    border-color: var(--lumo-warning-color);
  }
  .border-warning-10 {
    border-color: var(--lumo-warning-color-10pct);
  }
  .border-warning-strong {
    border-color: var(--lumo-warning-text-color);
  }

  /* === Border radius === */
  .rounded-none {
    border-radius: 0;
  }
  .rounded-s {
    border-radius: var(--lumo-border-radius-s);
  }
  .rounded-m {
    border-radius: var(--lumo-border-radius-m);
  }
  .rounded-l {
    border-radius: var(--lumo-border-radius-l);
  }
`;/**
 * @license
 * Copyright (c) 2017 - 2023 Vaadin Ltd.
 * This program is available under Apache License Version 2.0, available at https://vaadin.com/license/
 */const Tl=k`
  /* === Align content === */
  .content-center {
    align-content: center;
  }
  .content-end {
    align-content: flex-end;
  }
  .content-start {
    align-content: flex-start;
  }
  .content-around {
    align-content: space-around;
  }
  .content-between {
    align-content: space-between;
  }
  .content-evenly {
    align-content: space-evenly;
  }
  .content-stretch {
    align-content: stretch;
  }

  /* === Align items === */
  .items-baseline {
    align-items: baseline;
  }
  .items-center {
    align-items: center;
  }
  .items-end {
    align-items: flex-end;
  }
  .items-start {
    align-items: flex-start;
  }
  .items-stretch {
    align-items: stretch;
  }

  /* === Align self === */
  .self-auto {
    align-self: auto;
  }
  .self-baseline {
    align-self: baseline;
  }
  .self-center {
    align-self: center;
  }
  .self-end {
    align-self: flex-end;
  }
  .self-start {
    align-self: flex-start;
  }
  .self-stretch {
    align-self: stretch;
  }

  /* === Flex === */
  .flex-auto {
    flex: auto;
  }
  .flex-none {
    flex: none;
  }

  /* === Flex direction === */
  .flex-col {
    flex-direction: column;
  }
  .flex-col-reverse {
    flex-direction: column-reverse;
  }
  .flex-row {
    flex-direction: row;
  }
  .flex-row-reverse {
    flex-direction: row-reverse;
  }

  /* === Flex grow === */
  .flex-grow-0 {
    flex-grow: 0;
  }
  .flex-grow {
    flex-grow: 1;
  }

  /* === Flex shrink === */
  .flex-shrink-0 {
    flex-shrink: 0;
  }
  .flex-shrink {
    flex-shrink: 1;
  }

  /* === Flex wrap === */
  .flex-nowrap {
    flex-wrap: nowrap;
  }
  .flex-wrap {
    flex-wrap: wrap;
  }
  .flex-wrap-reverse {
    flex-wrap: wrap-reverse;
  }

  /* === Gap === */
  .gap-xs {
    gap: var(--lumo-space-xs);
  }
  .gap-s {
    gap: var(--lumo-space-s);
  }
  .gap-m {
    gap: var(--lumo-space-m);
  }
  .gap-l {
    gap: var(--lumo-space-l);
  }
  .gap-xl {
    gap: var(--lumo-space-xl);
  }

  /* === Gap (column) === */
  .gap-x-xs {
    column-gap: var(--lumo-space-xs);
  }
  .gap-x-s {
    column-gap: var(--lumo-space-s);
  }
  .gap-x-m {
    column-gap: var(--lumo-space-m);
  }
  .gap-x-l {
    column-gap: var(--lumo-space-l);
  }
  .gap-x-xl {
    column-gap: var(--lumo-space-xl);
  }

  /* === Gap (row) === */
  .gap-y-xs {
    row-gap: var(--lumo-space-xs);
  }
  .gap-y-s {
    row-gap: var(--lumo-space-s);
  }
  .gap-y-m {
    row-gap: var(--lumo-space-m);
  }
  .gap-y-l {
    row-gap: var(--lumo-space-l);
  }
  .gap-y-xl {
    row-gap: var(--lumo-space-xl);
  }

  /* === Grid auto flow === */
  .grid-flow-col {
    grid-auto-flow: column;
  }
  .grid-flow-row {
    grid-auto-flow: row;
  }

  /* === Grid columns === */
  .grid-cols-1 {
    grid-template-columns: repeat(1, minmax(0, 1fr));
  }
  .grid-cols-2 {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
  .grid-cols-3 {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
  .grid-cols-4 {
    grid-template-columns: repeat(4, minmax(0, 1fr));
  }
  .grid-cols-5 {
    grid-template-columns: repeat(5, minmax(0, 1fr));
  }
  .grid-cols-6 {
    grid-template-columns: repeat(6, minmax(0, 1fr));
  }
  .grid-cols-7 {
    grid-template-columns: repeat(7, minmax(0, 1fr));
  }
  .grid-cols-8 {
    grid-template-columns: repeat(8, minmax(0, 1fr));
  }
  .grid-cols-9 {
    grid-template-columns: repeat(9, minmax(0, 1fr));
  }
  .grid-cols-10 {
    grid-template-columns: repeat(10, minmax(0, 1fr));
  }
  .grid-cols-11 {
    grid-template-columns: repeat(11, minmax(0, 1fr));
  }
  .grid-cols-12 {
    grid-template-columns: repeat(12, minmax(0, 1fr));
  }

  /* === Grid rows === */
  .grid-rows-1 {
    grid-template-rows: repeat(1, minmax(0, 1fr));
  }
  .grid-rows-2 {
    grid-template-rows: repeat(2, minmax(0, 1fr));
  }
  .grid-rows-3 {
    grid-template-rows: repeat(3, minmax(0, 1fr));
  }
  .grid-rows-4 {
    grid-template-rows: repeat(4, minmax(0, 1fr));
  }
  .grid-rows-5 {
    grid-template-rows: repeat(5, minmax(0, 1fr));
  }
  .grid-rows-6 {
    grid-template-rows: repeat(6, minmax(0, 1fr));
  }

  /* === Justify content === */
  .justify-center {
    justify-content: center;
  }
  .justify-end {
    justify-content: flex-end;
  }
  .justify-start {
    justify-content: flex-start;
  }
  .justify-around {
    justify-content: space-around;
  }
  .justify-between {
    justify-content: space-between;
  }
  .justify-evenly {
    justify-content: space-evenly;
  }

  /* === Span (column) === */
  .col-span-1 {
    grid-column: span 1 / span 1;
  }
  .col-span-2 {
    grid-column: span 2 / span 2;
  }
  .col-span-3 {
    grid-column: span 3 / span 3;
  }
  .col-span-4 {
    grid-column: span 4 / span 4;
  }
  .col-span-5 {
    grid-column: span 5 / span 5;
  }
  .col-span-6 {
    grid-column: span 6 / span 6;
  }
  .col-span-7 {
    grid-column: span 7 / span 7;
  }
  .col-span-8 {
    grid-column: span 8 / span 8;
  }
  .col-span-9 {
    grid-column: span 9 / span 9;
  }
  .col-span-10 {
    grid-column: span 10 / span 10;
  }
  .col-span-11 {
    grid-column: span 11 / span 11;
  }
  .col-span-12 {
    grid-column: span 12 / span 12;
  }

  /* === Span (row) === */
  .row-span-1 {
    grid-row: span 1 / span 1;
  }
  .row-span-2 {
    grid-row: span 2 / span 2;
  }
  .row-span-3 {
    grid-row: span 3 / span 3;
  }
  .row-span-4 {
    grid-row: span 4 / span 4;
  }
  .row-span-5 {
    grid-row: span 5 / span 5;
  }
  .row-span-6 {
    grid-row: span 6 / span 6;
  }

  /* === Responsive design === */
  @media (min-width: 640px) {
    .sm\\:flex-col {
      flex-direction: column;
    }
    .sm\\:flex-row {
      flex-direction: row;
    }
    .sm\\:grid-cols-1 {
      grid-template-columns: repeat(1, minmax(0, 1fr));
    }
    .sm\\:grid-cols-2 {
      grid-template-columns: repeat(2, minmax(0, 1fr));
    }
    .sm\\:grid-cols-3 {
      grid-template-columns: repeat(3, minmax(0, 1fr));
    }
    .sm\\:grid-cols-4 {
      grid-template-columns: repeat(4, minmax(0, 1fr));
    }
    .sm\\:grid-cols-5 {
      grid-template-columns: repeat(5, minmax(0, 1fr));
    }
    .sm\\:grid-cols-6 {
      grid-template-columns: repeat(6, minmax(0, 1fr));
    }
    .sm\\:grid-cols-7 {
      grid-template-columns: repeat(7, minmax(0, 1fr));
    }
    .sm\\:grid-cols-8 {
      grid-template-columns: repeat(8, minmax(0, 1fr));
    }
    .sm\\:grid-cols-9 {
      grid-template-columns: repeat(9, minmax(0, 1fr));
    }
    .sm\\:grid-cols-10 {
      grid-template-columns: repeat(10, minmax(0, 1fr));
    }
    .sm\\:grid-cols-11 {
      grid-template-columns: repeat(11, minmax(0, 1fr));
    }
    .sm\\:grid-cols-12 {
      grid-template-columns: repeat(12, minmax(0, 1fr));
    }
  }

  @media (min-width: 768px) {
    .md\\:flex-col {
      flex-direction: column;
    }
    .md\\:flex-row {
      flex-direction: row;
    }
    .md\\:grid-cols-1 {
      grid-template-columns: repeat(1, minmax(0, 1fr));
    }
    .md\\:grid-cols-2 {
      grid-template-columns: repeat(2, minmax(0, 1fr));
    }
    .md\\:grid-cols-3 {
      grid-template-columns: repeat(3, minmax(0, 1fr));
    }
    .md\\:grid-cols-4 {
      grid-template-columns: repeat(4, minmax(0, 1fr));
    }
    .md\\:grid-cols-5 {
      grid-template-columns: repeat(5, minmax(0, 1fr));
    }
    .md\\:grid-cols-6 {
      grid-template-columns: repeat(6, minmax(0, 1fr));
    }
    .md\\:grid-cols-7 {
      grid-template-columns: repeat(7, minmax(0, 1fr));
    }
    .md\\:grid-cols-8 {
      grid-template-columns: repeat(8, minmax(0, 1fr));
    }
    .md\\:grid-cols-9 {
      grid-template-columns: repeat(9, minmax(0, 1fr));
    }
    .md\\:grid-cols-10 {
      grid-template-columns: repeat(10, minmax(0, 1fr));
    }
    .md\\:grid-cols-11 {
      grid-template-columns: repeat(11, minmax(0, 1fr));
    }
    .md\\:grid-cols-12 {
      grid-template-columns: repeat(12, minmax(0, 1fr));
    }
  }
  @media (min-width: 1024px) {
    .lg\\:flex-col {
      flex-direction: column;
    }
    .lg\\:flex-row {
      flex-direction: row;
    }
    .lg\\:grid-cols-1 {
      grid-template-columns: repeat(1, minmax(0, 1fr));
    }
    .lg\\:grid-cols-2 {
      grid-template-columns: repeat(2, minmax(0, 1fr));
    }
    .lg\\:grid-cols-3 {
      grid-template-columns: repeat(3, minmax(0, 1fr));
    }
    .lg\\:grid-cols-4 {
      grid-template-columns: repeat(4, minmax(0, 1fr));
    }
    .lg\\:grid-cols-5 {
      grid-template-columns: repeat(5, minmax(0, 1fr));
    }
    .lg\\:grid-cols-6 {
      grid-template-columns: repeat(6, minmax(0, 1fr));
    }
    .lg\\:grid-cols-7 {
      grid-template-columns: repeat(7, minmax(0, 1fr));
    }
    .lg\\:grid-cols-8 {
      grid-template-columns: repeat(8, minmax(0, 1fr));
    }
    .lg\\:grid-cols-9 {
      grid-template-columns: repeat(9, minmax(0, 1fr));
    }
    .lg\\:grid-cols-10 {
      grid-template-columns: repeat(10, minmax(0, 1fr));
    }
    .lg\\:grid-cols-11 {
      grid-template-columns: repeat(11, minmax(0, 1fr));
    }
    .lg\\:grid-cols-12 {
      grid-template-columns: repeat(12, minmax(0, 1fr));
    }
  }
  @media (min-width: 1280px) {
    .xl\\:flex-col {
      flex-direction: column;
    }
    .xl\\:flex-row {
      flex-direction: row;
    }
    .xl\\:grid-cols-1 {
      grid-template-columns: repeat(1, minmax(0, 1fr));
    }
    .xl\\:grid-cols-2 {
      grid-template-columns: repeat(2, minmax(0, 1fr));
    }
    .xl\\:grid-cols-3 {
      grid-template-columns: repeat(3, minmax(0, 1fr));
    }
    .xl\\:grid-cols-4 {
      grid-template-columns: repeat(4, minmax(0, 1fr));
    }
    .xl\\:grid-cols-5 {
      grid-template-columns: repeat(5, minmax(0, 1fr));
    }
    .xl\\:grid-cols-6 {
      grid-template-columns: repeat(6, minmax(0, 1fr));
    }
    .xl\\:grid-cols-7 {
      grid-template-columns: repeat(7, minmax(0, 1fr));
    }
    .xl\\:grid-cols-8 {
      grid-template-columns: repeat(8, minmax(0, 1fr));
    }
    .xl\\:grid-cols-9 {
      grid-template-columns: repeat(9, minmax(0, 1fr));
    }
    .xl\\:grid-cols-10 {
      grid-template-columns: repeat(10, minmax(0, 1fr));
    }
    .xl\\:grid-cols-11 {
      grid-template-columns: repeat(11, minmax(0, 1fr));
    }
    .xl\\:grid-cols-12 {
      grid-template-columns: repeat(12, minmax(0, 1fr));
    }
  }
  @media (min-width: 1536px) {
    .\\32xl\\:flex-col {
      flex-direction: column;
    }
    .\\32xl\\:flex-row {
      flex-direction: row;
    }
    .\\32xl\\:grid-cols-1 {
      grid-template-columns: repeat(1, minmax(0, 1fr));
    }
    .\\32xl\\:grid-cols-2 {
      grid-template-columns: repeat(2, minmax(0, 1fr));
    }
    .\\32xl\\:grid-cols-3 {
      grid-template-columns: repeat(3, minmax(0, 1fr));
    }
    .\\32xl\\:grid-cols-4 {
      grid-template-columns: repeat(4, minmax(0, 1fr));
    }
    .\\32xl\\:grid-cols-5 {
      grid-template-columns: repeat(5, minmax(0, 1fr));
    }
    .\\32xl\\:grid-cols-6 {
      grid-template-columns: repeat(6, minmax(0, 1fr));
    }
    .\\32xl\\:grid-cols-7 {
      grid-template-columns: repeat(7, minmax(0, 1fr));
    }
    .\\32xl\\:grid-cols-8 {
      grid-template-columns: repeat(8, minmax(0, 1fr));
    }
    .\\32xl\\:grid-cols-9 {
      grid-template-columns: repeat(9, minmax(0, 1fr));
    }
    .\\32xl\\:grid-cols-10 {
      grid-template-columns: repeat(10, minmax(0, 1fr));
    }
    .\\32xl\\:grid-cols-11 {
      grid-template-columns: repeat(11, minmax(0, 1fr));
    }
    .\\32xl\\:grid-cols-12 {
      grid-template-columns: repeat(12, minmax(0, 1fr));
    }
  }
`;/**
 * @license
 * Copyright (c) 2017 - 2023 Vaadin Ltd.
 * This program is available under Apache License Version 2.0, available at https://vaadin.com/license/
 */const $l=k`
  /* === Box sizing === */
  .box-border {
    box-sizing: border-box;
  }
  .box-content {
    box-sizing: content-box;
  }

  /* === Display === */
  .block {
    display: block;
  }
  .flex {
    display: flex;
  }
  .hidden {
    display: none;
  }
  .inline {
    display: inline;
  }
  .inline-block {
    display: inline-block;
  }
  .inline-flex {
    display: inline-flex;
  }
  .inline-grid {
    display: inline-grid;
  }
  .grid {
    display: grid;
  }

  /* === Overflow === */
  .overflow-auto {
    overflow: auto;
  }
  .overflow-hidden {
    overflow: hidden;
  }
  .overflow-scroll {
    overflow: scroll;
  }

  /* === Position === */
  .absolute {
    position: absolute;
  }
  .fixed {
    position: fixed;
  }
  .static {
    position: static;
  }
  .sticky {
    position: sticky;
  }
  .relative {
    position: relative;
  }

  /* === Responsive design === */
  @media (min-width: 640px) {
    .sm\\:flex {
      display: flex;
    }
    .sm\\:hidden {
      display: none;
    }
  }
  @media (min-width: 768px) {
    .md\\:flex {
      display: flex;
    }
    .md\\:hidden {
      display: none;
    }
  }
  @media (min-width: 1024px) {
    .lg\\:flex {
      display: flex;
    }
    .lg\\:hidden {
      display: none;
    }
  }
  @media (min-width: 1280px) {
    .xl\\:flex {
      display: flex;
    }
    .xl\\:hidden {
      display: none;
    }
  }
  @media (min-width: 1536px) {
    .\\32xl\\:flex {
      display: flex;
    }
    .\\32xl\\:hidden {
      display: none;
    }
  }
`;/**
 * @license
 * Copyright (c) 2017 - 2023 Vaadin Ltd.
 * This program is available under Apache License Version 2.0, available at https://vaadin.com/license/
 */const jl=k`
  /* === Box shadows === */
  .shadow-xs {
    box-shadow: var(--lumo-box-shadow-xs);
  }
  .shadow-s {
    box-shadow: var(--lumo-box-shadow-s);
  }
  .shadow-m {
    box-shadow: var(--lumo-box-shadow-m);
  }
  .shadow-l {
    box-shadow: var(--lumo-box-shadow-l);
  }
  .shadow-xl {
    box-shadow: var(--lumo-box-shadow-xl);
  }
`;/**
 * @license
 * Copyright (c) 2017 - 2023 Vaadin Ltd.
 * This program is available under Apache License Version 2.0, available at https://vaadin.com/license/
 */const Al=k`
  /* === Height === */
  .h-0 {
    height: 0;
  }
  .h-xs {
    height: var(--lumo-size-xs);
  }
  .h-s {
    height: var(--lumo-size-s);
  }
  .h-m {
    height: var(--lumo-size-m);
  }
  .h-l {
    height: var(--lumo-size-l);
  }
  .h-xl {
    height: var(--lumo-size-xl);
  }
  .h-auto {
    height: auto;
  }
  .h-full {
    height: 100%;
  }
  .h-screen {
    height: 100vh;
  }

  /* === Height (max) === */
  .max-h-full {
    max-height: 100%;
  }
  .max-h-screen {
    max-height: 100vh;
  }

  /* === Height (min) === */
  .min-h-0 {
    min-height: 0;
  }
  .min-h-full {
    min-height: 100%;
  }
  .min-h-screen {
    min-height: 100vh;
  }

  /* === Icon sizing === */
  .icon-s {
    height: var(--lumo-icon-size-s);
    width: var(--lumo-icon-size-s);
  }
  .icon-m {
    height: var(--lumo-icon-size-m);
    width: var(--lumo-icon-size-m);
  }
  .icon-l {
    height: var(--lumo-icon-size-l);
    width: var(--lumo-icon-size-l);
  }

  /* === Width === */
  .w-xs {
    width: var(--lumo-size-xs);
  }
  .w-s {
    width: var(--lumo-size-s);
  }
  .w-m {
    width: var(--lumo-size-m);
  }
  .w-l {
    width: var(--lumo-size-l);
  }
  .w-xl {
    width: var(--lumo-size-xl);
  }
  .w-auto {
    width: auto;
  }
  .w-full {
    width: 100%;
  }

  /* === Width (max) === */
  .max-w-full {
    max-width: 100%;
  }
  .max-w-screen-sm {
    max-width: 640px;
  }
  .max-w-screen-md {
    max-width: 768px;
  }
  .max-w-screen-lg {
    max-width: 1024px;
  }
  .max-w-screen-xl {
    max-width: 1280px;
  }
  .max-w-screen-2xl {
    max-width: 1536px;
  }

  /* === Width (min) === */
  .min-w-0 {
    min-width: 0;
  }
  .min-w-full {
    min-width: 100%;
  }
`;/**
 * @license
 * Copyright (c) 2017 - 2023 Vaadin Ltd.
 * This program is available under Apache License Version 2.0, available at https://vaadin.com/license/
 */const Rl=k`
  /* === Margin === */
  .m-auto {
    margin: auto;
  }
  .m-0 {
    margin: 0;
  }
  .m-xs {
    margin: var(--lumo-space-xs);
  }
  .m-s {
    margin: var(--lumo-space-s);
  }
  .m-m {
    margin: var(--lumo-space-m);
  }
  .m-l {
    margin: var(--lumo-space-l);
  }
  .m-xl {
    margin: var(--lumo-space-xl);
  }

  /* === Margin (bottom) === */
  .mb-auto {
    margin-bottom: auto;
  }
  .mb-0 {
    margin-bottom: 0;
  }
  .mb-xs {
    margin-bottom: var(--lumo-space-xs);
  }
  .mb-s {
    margin-bottom: var(--lumo-space-s);
  }
  .mb-m {
    margin-bottom: var(--lumo-space-m);
  }
  .mb-l {
    margin-bottom: var(--lumo-space-l);
  }
  .mb-xl {
    margin-bottom: var(--lumo-space-xl);
  }

  /* === Margin (end) === */
  .me-auto {
    margin-inline-end: auto;
  }
  .me-0 {
    margin-inline-end: 0;
  }
  .me-xs {
    margin-inline-end: var(--lumo-space-xs);
  }
  .me-s {
    margin-inline-end: var(--lumo-space-s);
  }
  .me-m {
    margin-inline-end: var(--lumo-space-m);
  }
  .me-l {
    margin-inline-end: var(--lumo-space-l);
  }
  .me-xl {
    margin-inline-end: var(--lumo-space-xl);
  }

  /* === Margin (horizontal) === */
  .mx-auto {
    margin-left: auto;
    margin-right: auto;
  }
  .mx-0 {
    margin-left: 0;
    margin-right: 0;
  }
  .mx-xs {
    margin-left: var(--lumo-space-xs);
    margin-right: var(--lumo-space-xs);
  }
  .mx-s {
    margin-left: var(--lumo-space-s);
    margin-right: var(--lumo-space-s);
  }
  .mx-m {
    margin-left: var(--lumo-space-m);
    margin-right: var(--lumo-space-m);
  }
  .mx-l {
    margin-left: var(--lumo-space-l);
    margin-right: var(--lumo-space-l);
  }
  .mx-xl {
    margin-left: var(--lumo-space-xl);
    margin-right: var(--lumo-space-xl);
  }

  /* === Margin (left) === */
  .ml-auto {
    margin-left: auto;
  }
  .ml-0 {
    margin-left: 0;
  }
  .ml-xs {
    margin-left: var(--lumo-space-xs);
  }
  .ml-s {
    margin-left: var(--lumo-space-s);
  }
  .ml-m {
    margin-left: var(--lumo-space-m);
  }
  .ml-l {
    margin-left: var(--lumo-space-l);
  }
  .ml-xl {
    margin-left: var(--lumo-space-xl);
  }

  /* === Margin (right) === */
  .mr-auto {
    margin-right: auto;
  }
  .mr-0 {
    margin-right: 0;
  }
  .mr-xs {
    margin-right: var(--lumo-space-xs);
  }
  .mr-s {
    margin-right: var(--lumo-space-s);
  }
  .mr-m {
    margin-right: var(--lumo-space-m);
  }
  .mr-l {
    margin-right: var(--lumo-space-l);
  }
  .mr-xl {
    margin-right: var(--lumo-space-xl);
  }

  /* === Margin (start) === */
  .ms-auto {
    margin-inline-start: auto;
  }
  .ms-0 {
    margin-inline-start: 0;
  }
  .ms-xs {
    margin-inline-start: var(--lumo-space-xs);
  }
  .ms-s {
    margin-inline-start: var(--lumo-space-s);
  }
  .ms-m {
    margin-inline-start: var(--lumo-space-m);
  }
  .ms-l {
    margin-inline-start: var(--lumo-space-l);
  }
  .ms-xl {
    margin-inline-start: var(--lumo-space-xl);
  }

  /* === Margin (top) === */
  .mt-auto {
    margin-top: auto;
  }
  .mt-0 {
    margin-top: 0;
  }
  .mt-xs {
    margin-top: var(--lumo-space-xs);
  }
  .mt-s {
    margin-top: var(--lumo-space-s);
  }
  .mt-m {
    margin-top: var(--lumo-space-m);
  }
  .mt-l {
    margin-top: var(--lumo-space-l);
  }
  .mt-xl {
    margin-top: var(--lumo-space-xl);
  }

  /* === Margin (vertical) === */
  .my-auto {
    margin-bottom: auto;
    margin-top: auto;
  }
  .my-0 {
    margin-bottom: 0;
    margin-top: 0;
  }
  .my-xs {
    margin-bottom: var(--lumo-space-xs);
    margin-top: var(--lumo-space-xs);
  }
  .my-s {
    margin-bottom: var(--lumo-space-s);
    margin-top: var(--lumo-space-s);
  }
  .my-m {
    margin-bottom: var(--lumo-space-m);
    margin-top: var(--lumo-space-m);
  }
  .my-l {
    margin-bottom: var(--lumo-space-l);
    margin-top: var(--lumo-space-l);
  }
  .my-xl {
    margin-bottom: var(--lumo-space-xl);
    margin-top: var(--lumo-space-xl);
  }

  /* === Padding === */
  .p-0 {
    padding: 0;
  }
  .p-xs {
    padding: var(--lumo-space-xs);
  }
  .p-s {
    padding: var(--lumo-space-s);
  }
  .p-m {
    padding: var(--lumo-space-m);
  }
  .p-l {
    padding: var(--lumo-space-l);
  }
  .p-xl {
    padding: var(--lumo-space-xl);
  }

  /* === Padding (bottom) === */
  .pb-0 {
    padding-bottom: 0;
  }
  .pb-xs {
    padding-bottom: var(--lumo-space-xs);
  }
  .pb-s {
    padding-bottom: var(--lumo-space-s);
  }
  .pb-m {
    padding-bottom: var(--lumo-space-m);
  }
  .pb-l {
    padding-bottom: var(--lumo-space-l);
  }
  .pb-xl {
    padding-bottom: var(--lumo-space-xl);
  }

  /* === Padding (end) === */
  .pe-0 {
    padding-inline-end: 0;
  }
  .pe-xs {
    padding-inline-end: var(--lumo-space-xs);
  }
  .pe-s {
    padding-inline-end: var(--lumo-space-s);
  }
  .pe-m {
    padding-inline-end: var(--lumo-space-m);
  }
  .pe-l {
    padding-inline-end: var(--lumo-space-l);
  }
  .pe-xl {
    padding-inline-end: var(--lumo-space-xl);
  }

  /* === Padding (horizontal) === */
  .px-0 {
    padding-left: 0;
    padding-right: 0;
  }
  .px-xs {
    padding-left: var(--lumo-space-xs);
    padding-right: var(--lumo-space-xs);
  }
  .px-s {
    padding-left: var(--lumo-space-s);
    padding-right: var(--lumo-space-s);
  }
  .px-m {
    padding-left: var(--lumo-space-m);
    padding-right: var(--lumo-space-m);
  }
  .px-l {
    padding-left: var(--lumo-space-l);
    padding-right: var(--lumo-space-l);
  }
  .px-xl {
    padding-left: var(--lumo-space-xl);
    padding-right: var(--lumo-space-xl);
  }

  /* === Padding (left) === */
  .pl-0 {
    padding-left: 0;
  }
  .pl-xs {
    padding-left: var(--lumo-space-xs);
  }
  .pl-s {
    padding-left: var(--lumo-space-s);
  }
  .pl-m {
    padding-left: var(--lumo-space-m);
  }
  .pl-l {
    padding-left: var(--lumo-space-l);
  }
  .pl-xl {
    padding-left: var(--lumo-space-xl);
  }

  /* === Padding (right) === */
  .pr-0 {
    padding-right: 0;
  }
  .pr-xs {
    padding-right: var(--lumo-space-xs);
  }
  .pr-s {
    padding-right: var(--lumo-space-s);
  }
  .pr-m {
    padding-right: var(--lumo-space-m);
  }
  .pr-l {
    padding-right: var(--lumo-space-l);
  }
  .pr-xl {
    padding-right: var(--lumo-space-xl);
  }

  /* === Padding (start) === */
  .ps-0 {
    padding-inline-start: 0;
  }
  .ps-xs {
    padding-inline-start: var(--lumo-space-xs);
  }
  .ps-s {
    padding-inline-start: var(--lumo-space-s);
  }
  .ps-m {
    padding-inline-start: var(--lumo-space-m);
  }
  .ps-l {
    padding-inline-start: var(--lumo-space-l);
  }
  .ps-xl {
    padding-inline-start: var(--lumo-space-xl);
  }

  /* === Padding (top) === */
  .pt-0 {
    padding-top: 0;
  }
  .pt-xs {
    padding-top: var(--lumo-space-xs);
  }
  .pt-s {
    padding-top: var(--lumo-space-s);
  }
  .pt-m {
    padding-top: var(--lumo-space-m);
  }
  .pt-l {
    padding-top: var(--lumo-space-l);
  }
  .pt-xl {
    padding-top: var(--lumo-space-xl);
  }

  /* === Padding (vertical) === */
  .py-0 {
    padding-bottom: 0;
    padding-top: 0;
  }
  .py-xs {
    padding-bottom: var(--lumo-space-xs);
    padding-top: var(--lumo-space-xs);
  }
  .py-s {
    padding-bottom: var(--lumo-space-s);
    padding-top: var(--lumo-space-s);
  }
  .py-m {
    padding-bottom: var(--lumo-space-m);
    padding-top: var(--lumo-space-m);
  }
  .py-l {
    padding-bottom: var(--lumo-space-l);
    padding-top: var(--lumo-space-l);
  }
  .py-xl {
    padding-bottom: var(--lumo-space-xl);
    padding-top: var(--lumo-space-xl);
  }
`;/**
 * @license
 * Copyright (c) 2017 - 2023 Vaadin Ltd.
 * This program is available under Apache License Version 2.0, available at https://vaadin.com/license/
 */const Nl=k`
  /* === Font size === */
  .text-2xs {
    font-size: var(--lumo-font-size-xxs);
  }
  .text-xs {
    font-size: var(--lumo-font-size-xs);
  }
  .text-s {
    font-size: var(--lumo-font-size-s);
  }
  .text-m {
    font-size: var(--lumo-font-size-m);
  }
  .text-l {
    font-size: var(--lumo-font-size-l);
  }
  .text-xl {
    font-size: var(--lumo-font-size-xl);
  }
  .text-2xl {
    font-size: var(--lumo-font-size-xxl);
  }
  .text-3xl {
    font-size: var(--lumo-font-size-xxxl);
  }

  /* === Font weight === */
  .font-thin {
    font-weight: 100;
  }
  .font-extralight {
    font-weight: 200;
  }
  .font-light {
    font-weight: 300;
  }
  .font-normal {
    font-weight: 400;
  }
  .font-medium {
    font-weight: 500;
  }
  .font-semibold {
    font-weight: 600;
  }
  .font-bold {
    font-weight: 700;
  }
  .font-extrabold {
    font-weight: 800;
  }
  .font-black {
    font-weight: 900;
  }

  /* === Line height === */
  .leading-none {
    line-height: 1;
  }
  .leading-xs {
    line-height: var(--lumo-line-height-xs);
  }
  .leading-s {
    line-height: var(--lumo-line-height-s);
  }
  .leading-m {
    line-height: var(--lumo-line-height-m);
  }

  /* === List style type === */
  .list-none {
    list-style-type: none;
  }

  /* === Text alignment === */
  .text-left {
    text-align: left;
  }
  .text-center {
    text-align: center;
  }
  .text-right {
    text-align: right;
  }
  .text-justify {
    text-align: justify;
  }

  /* === Text color === */
  .text-header {
    color: var(--lumo-header-text-color);
  }
  .text-body {
    color: var(--lumo-body-text-color);
  }
  .text-secondary {
    color: var(--lumo-secondary-text-color);
  }
  .text-tertiary {
    color: var(--lumo-tertiary-text-color);
  }
  .text-disabled {
    color: var(--lumo-disabled-text-color);
  }
  .text-primary {
    color: var(--lumo-primary-text-color);
  }
  .text-primary-contrast {
    color: var(--lumo-primary-contrast-color);
  }
  .text-error {
    color: var(--lumo-error-text-color);
  }
  .text-error-contrast {
    color: var(--lumo-error-contrast-color);
  }
  .text-success {
    color: var(--lumo-success-text-color);
  }
  .text-success-contrast {
    color: var(--lumo-success-contrast-color);
  }
  .text-warning {
    color: var(--lumo-warning-text-color);
  }
  .text-warning-contrast {
    color: var(--lumo-warning-contrast-color);
  }

  /* === Text overflow === */
  .overflow-clip {
    text-overflow: clip;
  }
  .overflow-ellipsis {
    text-overflow: ellipsis;
  }

  /* === Text transform === */
  .capitalize {
    text-transform: capitalize;
  }
  .lowercase {
    text-transform: lowercase;
  }
  .uppercase {
    text-transform: uppercase;
  }

  /* === Whitespace === */
  .whitespace-normal {
    white-space: normal;
  }
  .whitespace-nowrap {
    white-space: nowrap;
  }
  .whitespace-pre {
    white-space: pre;
  }
  .whitespace-pre-line {
    white-space: pre-line;
  }
  .whitespace-pre-wrap {
    white-space: pre-wrap;
  }

  /* === Responsive design === */
  @media (min-width: 640px) {
    .sm\\:text-2xs {
      font-size: var(--lumo-font-size-xxs);
    }
    .sm\\:text-xs {
      font-size: var(--lumo-font-size-xs);
    }
    .sm\\:text-s {
      font-size: var(--lumo-font-size-s);
    }
    .sm\\:text-m {
      font-size: var(--lumo-font-size-m);
    }
    .sm\\:text-l {
      font-size: var(--lumo-font-size-l);
    }
    .sm\\:text-xl {
      font-size: var(--lumo-font-size-xl);
    }
    .sm\\:text-2xl {
      font-size: var(--lumo-font-size-xxl);
    }
    .sm\\:text-3xl {
      font-size: var(--lumo-font-size-xxxl);
    }
  }
  @media (min-width: 768px) {
    .md\\:text-2xs {
      font-size: var(--lumo-font-size-xxs);
    }
    .md\\:text-xs {
      font-size: var(--lumo-font-size-xs);
    }
    .md\\:text-s {
      font-size: var(--lumo-font-size-s);
    }
    .md\\:text-m {
      font-size: var(--lumo-font-size-m);
    }
    .md\\:text-l {
      font-size: var(--lumo-font-size-l);
    }
    .md\\:text-xl {
      font-size: var(--lumo-font-size-xl);
    }
    .md\\:text-2xl {
      font-size: var(--lumo-font-size-xxl);
    }
    .md\\:text-3xl {
      font-size: var(--lumo-font-size-xxxl);
    }
  }
  @media (min-width: 1024px) {
    .lg\\:text-2xs {
      font-size: var(--lumo-font-size-xxs);
    }
    .lg\\:text-xs {
      font-size: var(--lumo-font-size-xs);
    }
    .lg\\:text-s {
      font-size: var(--lumo-font-size-s);
    }
    .lg\\:text-m {
      font-size: var(--lumo-font-size-m);
    }
    .lg\\:text-l {
      font-size: var(--lumo-font-size-l);
    }
    .lg\\:text-xl {
      font-size: var(--lumo-font-size-xl);
    }
    .lg\\:text-2xl {
      font-size: var(--lumo-font-size-xxl);
    }
    .lg\\:text-3xl {
      font-size: var(--lumo-font-size-xxxl);
    }
  }
  @media (min-width: 1280px) {
    .xl\\:text-2xs {
      font-size: var(--lumo-font-size-xxs);
    }
    .xl\\:text-xs {
      font-size: var(--lumo-font-size-xs);
    }
    .xl\\:text-s {
      font-size: var(--lumo-font-size-s);
    }
    .xl\\:text-m {
      font-size: var(--lumo-font-size-m);
    }
    .xl\\:text-l {
      font-size: var(--lumo-font-size-l);
    }
    .xl\\:text-xl {
      font-size: var(--lumo-font-size-xl);
    }
    .xl\\:text-2xl {
      font-size: var(--lumo-font-size-xxl);
    }
    .xl\\:text-3xl {
      font-size: var(--lumo-font-size-xxxl);
    }
  }
  @media (min-width: 1536px) {
    .\\32xl\\:text-2xs {
      font-size: var(--lumo-font-size-xxs);
    }
    .\\32xl\\:text-xs {
      font-size: var(--lumo-font-size-xs);
    }
    .\\32xl\\:text-s {
      font-size: var(--lumo-font-size-s);
    }
    .\\32xl\\:text-m {
      font-size: var(--lumo-font-size-m);
    }
    .\\32xl\\:text-l {
      font-size: var(--lumo-font-size-l);
    }
    .\\32xl\\:text-xl {
      font-size: var(--lumo-font-size-xl);
    }
    .\\32xl\\:text-2xl {
      font-size: var(--lumo-font-size-xxl);
    }
    .\\32xl\\:text-3xl {
      font-size: var(--lumo-font-size-xxxl);
    }
  }
`;/**
 * @license
 * Copyright (c) 2017 - 2023 Vaadin Ltd.
 * This program is available under Apache License Version 2.0, available at https://vaadin.com/license/
 */const Xo=k`
${Sl}
${Cl}
${zl}
${jl}
${Tl}
${$l}
${Al}
${Rl}
${Nl}
`;T("",Xo,{moduleId:"lumo-utility"});/**
 * @license
 * Copyright (c) 2017 - 2023 Vaadin Ltd.
 * This program is available under Apache License Version 2.0, available at https://vaadin.com/license/
 */ye("utility",Xo);const Il=t=>{const e=[];t!==document&&(e.push(Ke(Ko.cssText,"",t,!0)),e.push(Ke(Yo.cssText,"",t,!0)),e.push(Ke(Pa.cssText,"",t,!0)),e.push(Ke(Jo.cssText,"",t,!0)),e.push(Ke(Xo.cssText,"",t,!0)))},Pl=Il;Pl(document);export{cr as D,js as E,Z as I,j as L,te as N,ce as O,sr as P,Dl as T,Ll as X,Ml as Z,$o as _,ye as a,Ks as b,k as c,yl as d,Ee as e,lr as f,$ as g,y as h,Yo as i,Ko as j,Ke as k,N as l,Ts as m,ze as n,zr as o,Sr as p,T as r,Pe as s,Ra as t,A as u,Tr as w,$s as x,Cr as y};
