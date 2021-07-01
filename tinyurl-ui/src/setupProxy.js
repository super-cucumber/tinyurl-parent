const {createProxyMiddleware} = require('http-proxy-middleware');

module.exports = function(app) {
    app.use(createProxyMiddleware('/service', { target: 'http://api.tinyurl.vipgp88.com',changeOrigin:true,pathRewrite:{'^/service':''} }));
};