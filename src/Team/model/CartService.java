
package Team.model;

import java.util.ArrayList;
import java.util.List;

import Team.entity.CartItem;
import Team.entity.Product;

public class CartService {

	private final ProductModel model;
	private final List<CartItem> cart;

	public CartService(ProductModel model, List<CartItem> cart) {
		this.model = model;
		this.cart = cart;
	}

	private CartItem findInCart(int productId) {
		for (CartItem item : cart) {
			if (item.getProduct().getProductid() == productId) {
				return item;
			}
		}
		return null;
	}

	public String addToCart(int productId, int quantity) {
		synchronized (cart) {
			Product product = model.findById(productId);
			if (product == null) {
				return "該当する商品が見つかりませんでした。";
			}

			CartItem existing = findInCart(product.getProductid());
			int alreadyInCart = (existing != null) ? existing.getQuantity() : 0;
			int available = product.getStock() - alreadyInCart;

			if (quantity <= 0 || quantity > available) {
				return "在庫が足りません。追加できる数量: " + available;
			}

			if (existing != null) {
				existing.setQuantity(existing.getQuantity() + quantity);
			} else {
				cart.add(new CartItem(product, quantity));
			}

			return product.getName() + "をカートに追加しました。";
		}
	}

	public String decreaseQuantity(int productId) {
		synchronized (cart) {
			CartItem existing = findInCart(productId);
			if (existing == null) {
				return "カートに見つかりませんでした。";
			}
			if (existing.getQuantity() > 1) {
				existing.setQuantity(existing.getQuantity() - 1);
				return existing.getProduct().getName() + "の数量を変更しました。";
			}
			return removeFromCart(productId);
		}
	}

	public String removeFromCart(int productId) {
		synchronized (cart) {
			CartItem existing = findInCart(productId);
			if (existing == null) {
				return "カートに見つかりませんでした。";
			}
			cart.remove(existing);
			return existing.getProduct().getName() + "をカートから削除しました。";
		}
	}

	public CheckoutResult checkout() {
		synchronized (cart) {
			if (cart.isEmpty()) {
				return new CheckoutResult(false, 0, "カートが空です。商品を追加してください。", null);
			}

			List<CartItem> purchased = new ArrayList<CartItem>();
			StringBuilder failures = new StringBuilder();

			for (CartItem item : cart) {
				boolean ok = model.decreaseStock(item.getProduct().getProductid(), item.getQuantity());
				if (ok) {
					purchased.add(item);
				} else {
					failures.append(item.getProduct().getName()).append("は在庫不足のため注文できませんでした。カートに残しています。 ");
				}
			}

			if (purchased.isEmpty()) {
				return new CheckoutResult(false, 0, "注文できる商品がありませんでした。 " + failures.toString().trim(), null);
			}

			int total = 0;
			for (CartItem item : purchased) {
				total += item.getSubtotal();
			}

			cart.removeAll(purchased);

			String message = failures.length() > 0 ? failures.toString().trim() : null;
			return new CheckoutResult(true, total, message, purchased);
		}
	}

	public static class CheckoutResult {
		private final boolean success;
		private final int total;
		private final String message;
		private final List<CartItem> purchasedItems;

		public CheckoutResult(boolean success, int total, String message, List<CartItem> purchasedItems) {
			this.success = success;
			this.total = total;
			this.message = message;
			this.purchasedItems = purchasedItems;
		}

		public boolean isSuccess() {
			return success;
		}

		public int getTotal() {
			return total;
		}

		public String getMessage() {
			return message;
		}

		public List<CartItem> getPurchasedItems() {
			return purchasedItems;
		}
	}

}
