package Team.web;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import Team.entity.CartItem;
import Team.model.CartService;
import Team.view.ProductScreen;

public class CartServer {

	private final CartService cartService;
	private final List<CartItem> cart;
	private final int port;
	private HttpServer server;

	public CartServer(CartService cartService, List<CartItem> cart, int port) {
		this.cartService = cartService;
		this.cart = cart;
		this.port = port;
	}

	public void start() throws IOException {
		server = HttpServer.create(new InetSocketAddress(port), 0);
		server.createContext("/cart/add", this::handleAdd);
		server.createContext("/cart/decrease", this::handleDecrease);
		server.createContext("/cart/remove", this::handleRemove);
		server.createContext("/cart/checkout", this::handleCheckout);
		server.createContext("/cart", this::handleView);
		server.createContext("/", this::handleStatic);
		server.setExecutor(null);
		server.start();
		System.out.println("ブラウザ用カートサーバーを起動しました: http://localhost:" + port + "/");
	}

	public void stop() {
		if (server != null) {
			server.stop(0);
		}
	}

	private void handleStatic(HttpExchange exchange) throws IOException {
		String path = exchange.getRequestURI().getPath();
		if (path.equals("/")) {
			path = "/index.html";
		}

		Path base = Paths.get(".").toAbsolutePath().normalize();
		Path file = base.resolve("." + path).normalize();

		if (!file.startsWith(base) || !Files.exists(file) || Files.isDirectory(file)) {
			sendText(exchange, 404, "text/plain; charset=UTF-8", "Not Found");
			return;
		}

		byte[] bytes = Files.readAllBytes(file);
		String contentType = path.endsWith(".css") ? "text/css; charset=UTF-8" : "text/html; charset=UTF-8";
		sendBytes(exchange, 200, contentType, bytes);
	}

	private void handleAdd(HttpExchange exchange) throws IOException {
		Map<String, String> params = parseQuery(exchange.getRequestURI().getRawQuery());
		String message;
		try {
			int id = Integer.parseInt(params.getOrDefault("id", ""));
			int qty = Integer.parseInt(params.getOrDefault("qty", "1"));
			message = cartService.addToCart(id, qty);
		} catch (NumberFormatException e) {
			message = "入力が正しくありません。";
		}
		redirect(exchange, "/cart?msg=" + URLEncoder.encode(message, StandardCharsets.UTF_8));
	}

	private void handleDecrease(HttpExchange exchange) throws IOException {
		Map<String, String> params = parseQuery(exchange.getRequestURI().getRawQuery());
		String message;
		try {
			int id = Integer.parseInt(params.getOrDefault("id", ""));
			message = cartService.decreaseQuantity(id);
		} catch (NumberFormatException e) {
			message = "入力が正しくありません。";
		}
		redirect(exchange, "/cart?msg=" + URLEncoder.encode(message, StandardCharsets.UTF_8));
	}

	private void handleRemove(HttpExchange exchange) throws IOException {
		Map<String, String> params = parseQuery(exchange.getRequestURI().getRawQuery());
		String message;
		try {
			int id = Integer.parseInt(params.getOrDefault("id", ""));
			message = cartService.removeFromCart(id);
		} catch (NumberFormatException e) {
			message = "入力が正しくありません。";
		}
		redirect(exchange, "/cart?msg=" + URLEncoder.encode(message, StandardCharsets.UTF_8));
	}

	private void handleCheckout(HttpExchange exchange) throws IOException {
		CartService.CheckoutResult result = cartService.checkout();

		StringBuilder html = new StringBuilder();
		html.append("<html><head><meta charset='UTF-8'>");
		html.append("<meta name='viewport' content='width=device-width, initial-scale=1'>");
		html.append("<link rel='stylesheet' href='/style.css'></head><body>");
		writeNav(html, -1);
		html.append("<div class='detail-box'>");

		if (result.isSuccess()) {
			int deliveryDays = new Random().nextInt(6) + 2;
			DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("yyyy年MM月dd日");
			LocalDate orderDate = LocalDate.now();
			LocalDate deliveryDate = orderDate.plusDays(deliveryDays);

			html.append("<p>ご注文ありがとうございます！</p>");
			html.append("<p>注文日: ").append(orderDate.format(dateFormat)).append("</p>");

			html.append("<table>");
			html.append("<tr><th></th><th>商品名</th><th>単価</th><th>数量</th><th>小計</th></tr>");
			for (CartItem item : result.getPurchasedItems()) {
				html.append("<tr><td><img class='cart-thumb' src='").append(escapeHtml(item.getProduct().getImage()))
						.append("' alt='").append(escapeHtml(item.getProduct().getName())).append("'></td>")
						.append("<td>").append(escapeHtml(item.getProduct().getName())).append("</td>")
						.append("<td>¥").append(item.getProduct().getPrice()).append("</td>")
						.append("<td>").append(item.getQuantity()).append("</td>")
						.append("<td>¥").append(item.getSubtotal()).append("</td></tr>");
			}
			html.append("</table>");

			html.append("<p><strong>合計金額: ¥").append(result.getTotal()).append("</strong></p>");
			html.append("<p>お届け予定: ").append(deliveryDate.format(dateFormat)).append("（").append(deliveryDays)
					.append("日後）</p>");
			if (result.getMessage() != null) {
				html.append("<p>").append(escapeHtml(result.getMessage())).append("</p>");
			}
		} else {
			html.append("<p>").append(escapeHtml(result.getMessage())).append("</p>");
		}

		html.append("<a class='modoru-btn' href='/cart'>カートに戻る</a>");
		html.append("</div></body></html>");

		sendBytes(exchange, 200, "text/html; charset=UTF-8", html.toString().getBytes(StandardCharsets.UTF_8));
	}

	private void handleView(HttpExchange exchange) throws IOException {
		Map<String, String> params = parseQuery(exchange.getRequestURI().getRawQuery());
		String msg = params.get("msg");

		StringBuilder html = new StringBuilder();
		html.append("<html><head><meta charset='UTF-8'>");
		html.append("<meta name='viewport' content='width=device-width, initial-scale=1'>");
		html.append("<link rel='stylesheet' href='/style.css'></head><body>");
		writeNav(html, -1);
		html.append("<div class='detail-box'>");
		html.append("<h2>カートの中身</h2>");

		if (msg != null && !msg.isEmpty()) {
			html.append("<p class='cart-hint'>").append(escapeHtml(msg)).append("</p>");
		}

		synchronized (cart) {
			if (cart.isEmpty()) {
				html.append("<p>カートは空です。</p>");
			} else {
				html.append("<table>");
				html.append("<tr><th></th><th>商品名</th><th>単価</th><th>数量</th><th>小計</th><th></th></tr>");
				int total = 0;
				for (CartItem item : cart) {
					int productId = item.getProduct().getProductid();
					html.append("<tr><td><img class='cart-thumb' src='").append(escapeHtml(item.getProduct().getImage()))
							.append("' alt='").append(escapeHtml(item.getProduct().getName())).append("'></td>")
							.append("<td>").append(escapeHtml(item.getProduct().getName())).append("</td>")
							.append("<td>¥").append(item.getProduct().getPrice()).append("</td>")
							.append("<td><div class='qty-stepper'>")
							.append("<a class='step-btn' href='/cart/decrease?id=").append(productId).append("'>−</a>")
							.append("<span class='step-value'>").append(item.getQuantity()).append("</span>")
							.append("<a class='step-btn' href='/cart/add?id=").append(productId)
							.append("&qty=1'>＋</a>")
							.append("</div></td>")
							.append("<td>¥").append(item.getSubtotal()).append("</td>")
							.append("<td><a class='remove-btn' href='/cart/remove?id=").append(productId)
							.append("'>削除</a></td></tr>");
					total += item.getSubtotal();
				}
				html.append("</table>");
				html.append("<p><strong>合計金額: ¥").append(total).append("</strong></p>");
				html.append("<form method='get' action='/cart/checkout' onsubmit='return startCheckout(this)'>");
				html.append("<button type='submit' class='modoru-btn'>注文を確定する</button>");
				html.append("</form>");
			}
		}

		html.append("<a class='modoru-btn' href='/index.html'>商品一覧に戻る</a>");
		html.append("</div>");
		html.append("<div id='checkout-overlay' class='checkout-overlay'>");
		html.append("<div class='spinner'></div>");
		html.append("<p>支払い処理中...</p>");
		html.append("</div>");
		html.append("<script>");
		html.append("function startCheckout(form){");
		html.append("document.getElementById('checkout-overlay').classList.add('show');");
		html.append("setTimeout(function(){form.submit();},1400);");
		html.append("return false;");
		html.append("}");
		html.append("</script>");
		html.append("</body></html>");

		sendBytes(exchange, 200, "text/html; charset=UTF-8", html.toString().getBytes(StandardCharsets.UTF_8));
	}

	private void writeNav(StringBuilder html, int activeCategory) {
		html.append("<div class='site-top'>");
		html.append("<header><h1>ASE Shop</h1></header>");
		html.append("<div class='category-nav'>");
		html.append("<a class='cat-btn").append(activeCategory == 0 ? " active" : "").append("' href='/index.html'>すべて</a>");
		for (Map.Entry<Integer, String> entry : ProductScreen.CATEGORY_NAMES.entrySet()) {
			String activeClass = (entry.getKey() == activeCategory) ? " active" : "";
			html.append("<a class='cat-btn").append(activeClass).append("' href='/category-").append(entry.getKey())
					.append(".html'>").append(entry.getValue()).append("</a>");
		}
		html.append("<a class='cat-btn' href='/cart' style='margin-left:auto;'>").append(ProductScreen.CART_ICON_SVG)
				.append(" カート</a>");
		html.append("</div>");
		html.append("</div>");
	}

	private void redirect(HttpExchange exchange, String location) throws IOException {
		exchange.getResponseHeaders().set("Location", location);
		exchange.sendResponseHeaders(302, -1);
		exchange.close();
	}

	private void sendText(HttpExchange exchange, int status, String contentType, String body) throws IOException {
		sendBytes(exchange, status, contentType, body.getBytes(StandardCharsets.UTF_8));
	}

	private void sendBytes(HttpExchange exchange, int status, String contentType, byte[] bytes) throws IOException {
		exchange.getResponseHeaders().set("Content-Type", contentType);
		exchange.sendResponseHeaders(status, bytes.length);
		try (OutputStream os = exchange.getResponseBody()) {
			os.write(bytes);
		}
	}

	private Map<String, String> parseQuery(String rawQuery) {
		Map<String, String> map = new HashMap<String, String>();
		if (rawQuery == null) {
			return map;
		}
		for (String pair : rawQuery.split("&")) {
			int idx = pair.indexOf('=');
			String key = idx >= 0 ? pair.substring(0, idx) : pair;
			String value = idx >= 0 ? pair.substring(idx + 1) : "";
			map.put(URLDecoder.decode(key, StandardCharsets.UTF_8), URLDecoder.decode(value, StandardCharsets.UTF_8));
		}
		return map;
	}

	private static String escapeHtml(String s) {
		if (s == null) {
			return "";
		}
		return s.replace("&", "&amp;")
				.replace("<", "&lt;")
				.replace(">", "&gt;")
				.replace("\"", "&quot;")
				.replace("'", "&#39;");
	}

}
