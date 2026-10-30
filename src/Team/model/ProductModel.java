package Team.model;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;

import Team.entity.Product;

public class ProductModel {

	public ArrayList<Product> findAll() {

		ArrayList<Product> products = new ArrayList<Product>();

		try {
			Class.forName("org.sqlite.JDBC");

			String sql = "select * from products";

			try (Connection conn = DriverManager.getConnection("jdbc:sqlite:Teamwork03.db");
					PreparedStatement stmt = conn.prepareStatement(sql);
					ResultSet rs = stmt.executeQuery()) {

				while (rs.next()) {
					products.add(mapRow(rs));
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		return products;
	}

	public ArrayList<Product> findByCategory(int category) {

		ArrayList<Product> products = new ArrayList<Product>();

		try {
			Class.forName("org.sqlite.JDBC");

			String sql = "select * from products where category = ?";

			try (Connection conn = DriverManager.getConnection("jdbc:sqlite:Teamwork03.db");
					PreparedStatement stmt = conn.prepareStatement(sql)) {

				stmt.setInt(1, category);

				try (ResultSet rs = stmt.executeQuery()) {
					while (rs.next()) {
						products.add(mapRow(rs));
					}
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		return products;
	}

	public Product findById(int productId) {

		try {
			Class.forName("org.sqlite.JDBC");

			String sql = "select * from products where productid = ?";

			try (Connection conn = DriverManager.getConnection("jdbc:sqlite:Teamwork03.db");
					PreparedStatement stmt = conn.prepareStatement(sql)) {

				stmt.setInt(1, productId);

				try (ResultSet rs = stmt.executeQuery()) {
					if (rs.next()) {
						return mapRow(rs);
					}
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		return null;
	}

	public boolean decreaseStock(int productId, int quantity) {

		try {
			Class.forName("org.sqlite.JDBC");

			String sql = "update products set stock = stock - ? where productid = ? and stock >= ?";

			try (Connection conn = DriverManager.getConnection("jdbc:sqlite:Teamwork03.db");
					PreparedStatement stmt = conn.prepareStatement(sql)) {

				stmt.setInt(1, quantity);
				stmt.setInt(2, productId);
				stmt.setInt(3, quantity);

				int count = stmt.executeUpdate();
				return count > 0;
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		return false;
	}

	private Product mapRow(ResultSet rs) throws Exception {
		Product p = new Product();
		p.setProductid(rs.getInt("productid"));
		p.setCategory(rs.getInt("category"));
		p.setName(rs.getString("productname"));
		p.setDetails("");
		p.setPrice(rs.getInt("price"));
		p.setStock(rs.getInt("stock"));
		p.setImage(rs.getString("image"));
		return p;
	}

}
