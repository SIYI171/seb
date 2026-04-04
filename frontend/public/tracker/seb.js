(function() {
    var script = document.currentScript;
    var trackingId = script.getAttribute('data-tracking-id');
    var apiEndpoint = script.getAttribute('data-endpoint');
    
    if (!apiEndpoint) {
        var scriptSrc = script.src;
        var scriptOrigin = scriptSrc.substring(0, scriptSrc.indexOf('/', scriptSrc.indexOf('//') + 2));
        apiEndpoint = scriptOrigin.replace(/:\d+/, ':7322') + '/api/collect';
    }
    
    if (!trackingId) {
        console.error('SEB: Missing tracking-id');
        return;
    }
    
    function getSessionId() {
        var sessionId = sessionStorage.getItem('seb_session_id');
        if (!sessionId) {
            sessionId = 'seb_' + Date.now() + '_' + Math.random().toString(36).substr(2, 9);
            sessionStorage.setItem('seb_session_id', sessionId);
        }
        return sessionId;
    }
    
    function getBrowser() {
        var ua = navigator.userAgent;
        if (ua.indexOf('Firefox') > -1) return 'Firefox';
        if (ua.indexOf('Edg') > -1) return 'Edge';
        if (ua.indexOf('Chrome') > -1) return 'Chrome';
        if (ua.indexOf('Safari') > -1) return 'Safari';
        if (ua.indexOf('Opera') > -1 || ua.indexOf('OPR') > -1) return 'Opera';
        if (ua.indexOf('MSIE') > -1 || ua.indexOf('Trident') > -1) return 'IE';
        return 'Unknown';
    }
    
    function getOS() {
        var ua = navigator.userAgent;
        if (ua.indexOf('Windows') > -1) return 'Windows';
        if (ua.indexOf('Mac') > -1) return 'MacOS';
        if (ua.indexOf('Linux') > -1) return 'Linux';
        if (ua.indexOf('Android') > -1) return 'Android';
        if (ua.indexOf('iPhone') > -1 || ua.indexOf('iPad') > -1) return 'iOS';
        return 'Unknown';
    }
    
    function getDevice() {
        var ua = navigator.userAgent;
        if (/Mobi|Android|iPhone|iPad|iPod|BlackBerry|IEMobile|Opera Mini/i.test(ua)) {
            if (/iPad|Tablet/i.test(ua)) return 'Tablet';
            return 'Mobile';
        }
        return 'Desktop';
    }
    
    function collect() {
        var data = {
            trackingId: trackingId,
            sessionId: getSessionId(),
            url: window.location.href,
            referrer: document.referrer || '',
            browser: getBrowser(),
            os: getOS(),
            device: getDevice(),
            country: ''
        };
        
        var xhr = new XMLHttpRequest();
        xhr.open('POST', apiEndpoint, true);
        xhr.setRequestHeader('Content-Type', 'application/json');
        xhr.send(JSON.stringify(data));
    }
    
    if (document.readyState === 'complete') {
        collect();
    } else {
        window.addEventListener('load', collect);
    }
})();
