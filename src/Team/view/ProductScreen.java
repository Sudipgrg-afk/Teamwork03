package Team.view;

import java.awt.Desktop;
import java.io.File;
import java.io.FileWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Random;
import java.util.Set;

import Team.entity.Product;

public class ProductScreen {

	public static final LinkedHashMap<Integer, String> CATEGORY_NAMES = new LinkedHashMap<Integer, String>();
	static {
		CATEGORY_NAMES.put(1, "Tシャツ");
		CATEGORY_NAMES.put(2, "靴");
		CATEGORY_NAMES.put(3, "アクセサリー");
		CATEGORY_NAMES.put(4, "バッグ");
		CATEGORY_NAMES.put(5, "イヤホン");
		CATEGORY_NAMES.put(6, "スマートフォン");
		CATEGORY_NAMES.put(7, "ノートパソコン");
		CATEGORY_NAMES.put(8, "パンツ");
		CATEGORY_NAMES.put(9, "コスメ");
		CATEGORY_NAMES.put(10, "インテリア雑貨");
		CATEGORY_NAMES.put(11, "帽子");
		CATEGORY_NAMES.put(12, "カメラ");
	}

	public static final String CART_ICON_SVG = "<svg class='nav-icon' viewBox='0 0 24 24' fill='none' "
			+ "stroke='currentColor' stroke-width='2' stroke-linecap='round' stroke-linejoin='round' "
			+ "aria-hidden='true'><circle cx='9' cy='21' r='1'></circle><circle cx='20' cy='21' r='1'></circle>"
			+ "<path d='M1 1h4l2.68 13.39a2 2 0 0 0 2 1.61h9.72a2 2 0 0 0 2-1.61L23 6H6'></path></svg>";

	private final Set<Integer> writtenDetailIds = new HashSet<Integer>();

	public void showList(ArrayList<Product> products) {

		writtenDetailIds.clear();
		writePage("index.html", 0, products);

		try {
			Desktop.getDesktop().browse(new File("index.html").toURI());
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	public void showCategoryPage(int categoryId, ArrayList<Product> products) {
		writePage("category-" + categoryId + ".html", categoryId, products);
	}

	private void writePage(String fileName, int activeCategory, ArrayList<Product> products) {

		try {
			FileWriter fw = new FileWriter(fileName, StandardCharsets.UTF_8);
			fw.write("<html>");
			fw.write("<head>");
			fw.write("<meta charset='UTF-8'>");
			fw.write("<meta name='viewport' content='width=device-width, initial-scale=1'>");
			fw.write("<link rel='stylesheet' href='style.css'>");
			fw.write("</head>");
			fw.write("<body>");

			writeNav(fw, activeCategory, "");

			if (products.isEmpty()) {
				fw.write("<p style='padding:24px;'>該当する商品が見つかりませんでした。</p>");
			} else {
				fw.write("<div class='product-grid'>");
				for (Product p : products) {
					String categoryLabel = CATEGORY_NAMES.getOrDefault(p.getCategory(), "カテゴリ" + p.getCategory());
					fw.write("<div class='card'>");
					fw.write("<a class='card-link' href='Details/" + p.getProductid() + ".html'>");
					fw.write("<img src='" + escapeHtml(p.getImage()) + "' alt='" + escapeHtml(p.getName())
							+ "' loading='lazy'>");
					fw.write("<div class='name'>" + escapeHtml(p.getName()) + "</div>");
					fw.write("<div class='category'>" + escapeHtml(categoryLabel) + "</div>");
					fw.write(writeRating(p.getProductid()));
					fw.write("<div class='price'>¥" + p.getPrice() + "</div>");
					fw.write("<div class='stock'>在庫: " + p.getStock() + "</div>");
					fw.write("</a>");

					if (p.getStock() > 0) {
						fw.write("<form class='quick-add' method='get' action='http://localhost:8080/cart/add' "
								+ "target='cart-frame' onsubmit='return addToCart(this)'>");
						fw.write("<input type='hidden' name='id' value='" + p.getProductid() + "'>");
						fw.write("<input type='number' name='qty' value='1' min='1' max='" + p.getStock()
								+ "' aria-label='数量'>");
						fw.write("<button type='submit' class='modoru-btn'>カートに追加</button>");
						fw.write("</form>");
					} else {
						fw.write("<p class='out-of-stock'>在庫切れ</p>");
					}

					fw.write("</div>");

					showDetail(p);
				}
				fw.write("</div>");
			}

			writeCartHelpers(fw);

			fw.write("</body>");
			fw.write("</html>");
			fw.close();

			System.out.println(fileName + "を作成しました");

		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	public void showDetail(Product product) {

		if (!writtenDetailIds.add(product.getProductid())) {
			return;
		}

		File detailsDir = new File("Details");
		if (!detailsDir.exists()) {
			detailsDir.mkdirs();
		}

		try {
			FileWriter fw = new FileWriter("Details/" + product.getProductid() + ".html", StandardCharsets.UTF_8);
			fw.write("<html>");
			fw.write("<head>");
			fw.write("<meta charset='UTF-8'>");
			fw.write("<meta name='viewport' content='width=device-width, initial-scale=1'>");
			fw.write("<link rel='stylesheet' href='../style.css'>");
			fw.write("</head>");
			fw.write("<body>");

			writeNav(fw, product.getCategory(), "../");

			String categoryLabel = CATEGORY_NAMES.getOrDefault(product.getCategory(), "カテゴリ" + product.getCategory());

			fw.write("<div class='detail-box'>");
			fw.write("<img src='" + escapeHtml(product.getImage()) + "' alt='" + escapeHtml(product.getName())
					+ "' loading='lazy'>");
			fw.write("<h2>" + escapeHtml(product.getName()) + "</h2>");
			fw.write(writeRating(product.getProductid()));

			fw.write("<table>");
			fw.write("<tr><th>商品ID</th><td>" + product.getProductid() + "</td></tr>");
			fw.write("<tr><th>カテゴリ</th><td>" + escapeHtml(categoryLabel) + "</td></tr>");
			fw.write("<tr><th>価格</th><td>¥" + product.getPrice() + "</td></tr>");
			fw.write("<tr><th>在庫</th><td>" + product.getStock() + "</td></tr>");
			if (product.getDetails() != null && !product.getDetails().isEmpty()) {
				fw.write("<tr><th>詳細</th><td>" + escapeHtml(product.getDetails()) + "</td></tr>");
			}
			fw.write("</table>");

			fw.write("<form class='cart-hint' method='get' action='http://localhost:8080/cart/add' "
					+ "target='cart-frame' onsubmit='return addToCart(this)'>");
			fw.write("<input type='hidden' name='id' value='" + product.getProductid() + "'>");
			fw.write("<label>数量: <input type='number' name='qty' value='1' min='1' max='" + product.getStock()
					+ "' style='width:60px;'></label> ");
			fw.write("<button type='submit' class='modoru-btn' style='margin-top:0;'>カートに追加</button>");
			fw.write("</form>");
			fw.write("<a class='modoru-btn' href='http://localhost:8080/cart'>" + CART_ICON_SVG + " カートを見る</a> ");

			fw.write("<a class='modoru-btn' href='#' onclick='history.back()'>戻る</a>");
			fw.write("</div>");

			writeCartHelpers(fw);

			fw.write("</body>");
			fw.write("</html>");
			fw.close();

		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	private void writeNav(FileWriter fw, int activeCategory, String pathPrefix) throws Exception {
		fw.write("<div class='site-top'>");
		fw.write("<header><h1>ASE Shop</h1></header>");

		fw.write("<div class='category-nav'>");
		fw.write("<a class='cat-btn" + (activeCategory == 0 ? " active" : "") + "' href='" + pathPrefix
				+ "index.html'>すべて</a>");
		for (int categoryId : CATEGORY_NAMES.keySet()) {
			String label = CATEGORY_NAMES.get(categoryId);
			String activeClass = (categoryId == activeCategory) ? " active" : "";
			fw.write("<a class='cat-btn" + activeClass + "' href='" + pathPrefix + "category-" + categoryId
					+ ".html'>" + label + "</a>");
		}
		fw.write("<a class='cat-btn' href='http://localhost:8080/cart' style='margin-left:auto;'>" + CART_ICON_SVG
				+ " カート</a>");
		fw.write("</div>");
		fw.write("</div>");
	}

	private void writeCartHelpers(FileWriter fw) throws Exception {
		fw.write("<div id='toast' class='toast' aria-live='polite'></div>");
		fw.write("<iframe name='cart-frame' style='display:none'></iframe>");
		fw.write("<script>");
		fw.write("function addToCart(form){");
		fw.write("var card=form.closest('.card');");
		fw.write("var nameEl=card?card.querySelector('.name'):document.querySelector('h2');");
		fw.write("var name=nameEl?nameEl.textContent:'商品';");
		fw.write("var t=document.getElementById('toast');");
		fw.write("t.textContent=name+'をカートに追加しました';");
		fw.write("t.classList.add('show');");
		fw.write("clearTimeout(t._hideTimer);");
		fw.write("t._hideTimer=setTimeout(function(){t.classList.remove('show');},2000);");
		fw.write("return true;");
		fw.write("}");
		fw.write("</script>");
	}

	private static double ratingFor(int productId) {
		Random r = new Random(productId * 7919L);
		return 3.5 + r.nextInt(16) / 10.0;
	}

	private static int reviewCountFor(int productId) {
		Random r = new Random(productId * 104729L);
		return 3 + r.nextInt(848);
	}

	private static String writeRating(int productId) {
		double rating = ratingFor(productId);
		int reviews = reviewCountFor(productId);
		int filled = (int) Math.round(rating);
		StringBuilder stars = new StringBuilder();
		for (int i = 0; i < 5; i++) {
			stars.append(i < filled ? "★" : "☆");
		}
		return "<div class='rating' title='この評価はサンプルデータです（実際のレビューではありません）'>" + stars
				+ " <span class='rating-value'>" + String.format(Locale.JAPAN, "%.1f", rating)
				+ "</span> <span class='review-count'>(" + reviews + "件)</span> "
				+ "<span class='rating-sample-tag'>サンプル</span></div>";
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
