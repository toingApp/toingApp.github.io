local activit,  luaBridge, lay = ...


Toast = luajava.bindClass("android.widget.Toast")
local TextView = luajava.bindClass("android.widget.TextView")

text = "g"
toast = Toast:makeText(activit, text, Toast.LENGTH_LONG)



Lin = lay
Lin:setBackgroundColor(0x0000ff00)

local tc = TextView.new(activit)

tc:setText("ggu")

WebView = luajava.bindClass("android.webkit.WebView")
WebViewClient = luajava.bindClass("android.webkit.WebViewClient")
wb = WebView.new(activit)
LinearLayout = luajava.bindClass("android.widget.LinearLayout")
pr = LinearLayout.LayoutParams.new(LinearLayout.LayoutParams.FILL_PARENT, 200)

wc = WebViewClient.new()
wst = wb:getSettings()
wst:setJavaScriptEnabled(true)
wst:setAllowFileAccess(true)
wst:setUserAgentString("Mozilla/4.0 (compatible; MSIE 5.01; Windows NT 5.0)")
wst:setAppCacheEnabled(true);
wst:setBuiltInZoomControls(true);
wst:setSaveFormData(true)
wb:setWebViewClient(wc)
wb:setLayoutParams(pr)
wb:setBackgroundColor(0x0000ff00)

texte = "<html><head><style> body, html{background:transparent;color:white}</style></head><body><div align='center'><marquee>GT TUNNEL SIEMPRE LO MEJOR</marquee></div></body></html>"
texts ="https://raw.githubusercontent.com/toingApp/toingApp.github.io/main/files/tarjeta-de-felicitaci%C3%B3n-del-d%C3%ADa-la-independencia-septiembre-guatemala-fondo-celebraci%C3%B3n-con-los-fuegos-artificiales-las-150905515.jpg"
wb:loadUrl(texts)

Lin:addView(wb)
