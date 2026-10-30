package Team.controller;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import Team.entity.CartItem;
import Team.entity.Product;
import Team.model.CartService;
import Team.model.ProductModel;
import Team.view.ProductScreen;
import Team.web.CartServer;

public class MainController {

	private static final int CART_SERVER_PORT = 8080;

	public static void main(String[] args) {

		ProductModel model = new ProductModel();
		ProductScreen view = new ProductScreen();

		List<CartItem> cart = Collections.synchronizedList(new ArrayList<CartItem>());
		CartService cartService = new CartService(model, cart);

		CartServer cartServer = new CartServer(cartService, cart, CART_SERVER_PORT);
		try {
			cartServer.start();
		} catch (Exception e) {
			System.out.println("ブラウザ用カートサーバーを起動できませんでした。カート機能は利用できません。");
			e.printStackTrace();
		}

		Scanner sc = new Scanner(System.in);

		System.out.println("****ASE Shop****");

		while (true) {

			System.out.println();
			System.out.println("=== MENU ===");
			System.out.println("---------------------------");
			System.out.println(" 1: ショップを開く（ブラウザ）");
			System.out.println(" 0: 終了");
			System.out.println("---------------------------");
			int menu = readInt(sc, "番号を選択してください > ");

			if (menu == 1) {
				ArrayList<Product> products = model.findAll();
				view.showList(products);

				for (Map.Entry<Integer, String> entry : ProductScreen.CATEGORY_NAMES.entrySet()) {
					ArrayList<Product> categoryProducts = model.findByCategory(entry.getKey());
					view.showCategoryPage(entry.getKey(), categoryProducts);
				}

			} else if (menu == 0) {
				System.out.println("終了します。。。。。");
				break;

			} else {
				System.out.println("無効な番号です。もう一度入力してください。");
			}
		}

		cartServer.stop();
		sc.close();
	}

	private static int readInt(Scanner sc, String prompt) {
		while (true) {
			System.out.print(prompt);
			if (sc.hasNextInt()) {
				int value = sc.nextInt();
				sc.nextLine();
				return value;
			}
			System.out.println("数字を入力してください。");
			sc.nextLine();
		}
	}

}
