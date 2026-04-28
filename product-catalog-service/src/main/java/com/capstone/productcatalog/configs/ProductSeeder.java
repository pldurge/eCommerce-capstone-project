package com.capstone.productcatalog.configs;

import com.capstone.productcatalog.models.Category;
import com.capstone.productcatalog.models.Product;
import com.capstone.productcatalog.models.ProductDocument;
import com.capstone.productcatalog.repositories.CategoryRepository;
import com.capstone.productcatalog.repositories.ProductRepository;
import com.capstone.productcatalog.repositories.ProductSearchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class ProductSeeder implements CommandLineRunner {

    private final CategoryRepository categoryRepository;
    private final ProductRepository  productRepository;
    private final ProductSearchRepository searchRepository;

    @Override
    @Transactional
    public void run(String... args) {
        // Idempotent — skip if products already exist
        if (productRepository.count() > 0) {
            log.info("Products already seeded ({} found) — skipping", productRepository.count());
            return;
        }

        log.info("Seeding categories and products...");

        // ── Categories ─────────────────────────────────────────────────────
        Category electronics  = getOrCreateCategory("Electronics",
                "Smartphones, headphones, laptops, TVs and all consumer electronics");
        Category clothing     = getOrCreateCategory("Clothing",
                "Men's, women's and unisex fashion — shirts, jeans, shoes and outerwear");
        Category books        = getOrCreateCategory("Books",
                "Programming, self-help, business and general non-fiction titles");
        Category homeKitchen  = getOrCreateCategory("Home & Kitchen",
                "Appliances, furniture, cookware and home décor essentials");

        // ── Products ───────────────────────────────────────────────────────
        List<Product> products = List.of(

                // ── Electronics (5) ───────────────────────────────────────────
                product("Samsung Galaxy S24 Ultra",
                        "6.8-inch QHD+ Dynamic AMOLED, Snapdragon 8 Gen 3, 200MP camera, S Pen included. " +
                                "12GB RAM, 256GB storage, 5000mAh battery with 45W fast charging.",
                        new BigDecimal("134999.00"), 50, electronics,
                        "https://images.samsung.com/in/smartphones/galaxy-s24-ultra/images/galaxy-s24-ultra-highlights-titaniumblack.jpg"),

                product("Apple AirPods Pro (2nd Gen)",
                        "Active noise cancellation, Adaptive Transparency, Personalised Spatial Audio. " +
                                "Up to 6 hours listening time (30 hours with case). MagSafe charging case.",
                        new BigDecimal("24900.00"), 100, electronics,
                        "https://store.storeimages.cdn-apple.com/4668/as-images.apple.com/is/MQD83"),

                product("Sony WH-1000XM5 Headphones",
                        "Industry-leading noise cancellation with dual noise sensor technology. " +
                                "30-hour battery, Speak-to-Chat, 360 Reality Audio. Multipoint Bluetooth connection.",
                        new BigDecimal("29990.00"), 75, electronics,
                        "https://www.sony.co.in/image/wh1000xm5-black-front"),

                product("Logitech MX Master 3S Mouse",
                        "8K DPI optical sensor, MagSpeed electromagnetic scrolling, quiet clicks. " +
                                "Ergonomic design, 70-day battery life. Works on glass. USB-C charging.",
                        new BigDecimal("8995.00"), 150, electronics,
                        "https://resource.logitech.com/w_692,c_lpad,ar_4:3,q_auto,f_auto/d_transparent.gif/content/dam/logitech/en/products/mice/mx-master-3s/gallery/mx-master-3s-mouse-top-view-graphite.png"),

                product("Samsung 55-inch 4K QLED TV",
                        "Quantum Dot technology, 4K upscaling, HDR10+. 120Hz refresh rate, " +
                                "Object Tracking Sound, Tizen OS with built-in streaming apps. 3x HDMI 2.1.",
                        new BigDecimal("89999.00"), 30, electronics,
                        "https://images.samsung.com/in/tvs/qled-tv/qled-4k/q60c"),

                // ── Clothing (5) ──────────────────────────────────────────────
                product("Men's Classic Oxford Shirt",
                        "100% premium cotton Oxford weave. Button-down collar, chest pocket. " +
                                "Regular fit, machine washable. Available in white, light blue and grey.",
                        new BigDecimal("1499.00"), 200, clothing,
                        "https://images.unsplash.com/photo-1598033129183-c4f50c736f10?w=400"),

                product("Women's Slim Fit Jeans",
                        "Mid-rise slim fit denim with 2% elastane for comfort stretch. " +
                                "5-pocket styling, zip fly. Ankle length. Available in dark blue and black.",
                        new BigDecimal("2199.00"), 180, clothing,
                        "https://images.unsplash.com/photo-1541099649105-f69ad21f3246?w=400"),

                product("Unisex Running Shoes",
                        "Lightweight mesh upper with breathable construction. EVA midsole for cushioning, " +
                                "rubber outsole for grip. Lace-up closure. Suitable for road running and gym.",
                        new BigDecimal("3499.00"), 120, clothing,
                        "https://images.unsplash.com/photo-1542291026-7eec264c27ff?w=400"),

                product("Men's Winter Puffer Jacket",
                        "100g recycled polyester fill, windproof and water-resistant shell. " +
                                "Stand-up collar, two zip pockets, elasticated cuffs. Packable into pocket.",
                        new BigDecimal("4999.00"), 80, clothing,
                        "https://images.unsplash.com/photo-1591047139829-d91aecb6caea?w=400"),

                product("Women's Floral Maxi Dress",
                        "Lightweight viscose fabric with allover floral print. V-neck, adjustable " +
                                "spaghetti straps, tiered skirt. Fully lined. Ideal for summer occasions.",
                        new BigDecimal("1899.00"), 150, clothing,
                        "https://images.unsplash.com/photo-1572804013309-59a88b7e92f1?w=400"),

                // ── Books (5) ─────────────────────────────────────────────────
                product("Clean Code by Robert C. Martin",
                        "A handbook of agile software craftsmanship. Covers writing readable, " +
                                "maintainable code with examples in Java. Essential reading for every developer.",
                        new BigDecimal("699.00"), 300, books,
                        "https://images-na.ssl-images-amazon.com/images/I/41xShlnTZTL._SX376_BO1,204,203,200_.jpg"),

                product("The Pragmatic Programmer",
                        "From journeyman to master — 20th anniversary edition. Covers career development, " +
                                "software architecture, testing and team dynamics. Timeless advice for engineers.",
                        new BigDecimal("849.00"), 250, books,
                        "https://images-na.ssl-images-amazon.com/images/I/41as+WafrFL._SX376_BO1,204,203,200_.jpg"),

                product("Designing Data-Intensive Applications",
                        "The big ideas behind reliable, scalable and maintainable systems. Covers " +
                                "databases, stream processing, distributed systems. By Martin Kleppmann.",
                        new BigDecimal("1199.00"), 200, books,
                        "https://images-na.ssl-images-amazon.com/images/I/51ZSpMl1-LL._SX379_BO1,204,203,200_.jpg"),

                product("Atomic Habits by James Clear",
                        "An easy and proven way to build good habits and break bad ones. " +
                                "Practical strategies backed by biology, psychology and neuroscience.",
                        new BigDecimal("499.00"), 400, books,
                        "https://images-na.ssl-images-amazon.com/images/I/51-nXsSRfZL._SX329_BO1,204,203,200_.jpg"),

                product("The Psychology of Money",
                        "Timeless lessons on wealth, greed and happiness. Morgan Housel explores " +
                                "how people think about money and how that shapes financial decisions.",
                        new BigDecimal("399.00"), 350, books,
                        "https://images-na.ssl-images-amazon.com/images/I/41r6V0JJinL._SX327_BO1,204,203,200_.jpg"),

                // ── Home & Kitchen (5) ────────────────────────────────────────
                product("Instant Pot Duo 7-in-1 Pressure Cooker",
                        "Pressure cooker, slow cooker, rice cooker, steamer, sauté pan, yogurt maker " +
                                "and food warmer in one appliance. 5.7L capacity. Stainless steel inner pot.",
                        new BigDecimal("8999.00"), 60, homeKitchen,
                        "https://images.unsplash.com/photo-1585515320310-259814833e62?w=400"),

                product("Philips Air Fryer HD9200",
                        "Rapid Air technology for crispy food with up to 90% less fat. 4.1L capacity, " +
                                "7 pre-set programmes, digital touch screen. Dishwasher-safe drawer and basket.",
                        new BigDecimal("6495.00"), 90, homeKitchen,
                        "https://www.philips.co.in/c-dam/b2c/en/fam/airfryer/images/hd9200"),

                product("IKEA LACK Coffee Table",
                        "Simple, clean design that suits any living room. Hollow construction keeps it " +
                                "lightweight. 90x55 cm surface. Available in white, black-brown and oak effect.",
                        new BigDecimal("3999.00"), 40, homeKitchen,
                        "https://www.ikea.com/in/en/images/products/lack-coffee-table-white__0580688_pe669122_s5.jpg"),

                product("Dyson V12 Detect Slim Absolute",
                        "Laser dust detection reveals invisible dust. 150 AW suction, up to 60 min runtime. " +
                                "HEPA filtration, LCD screen shows real-time dust count. 7 accessories included.",
                        new BigDecimal("54900.00"), 25, homeKitchen,
                        "https://dyson-h.assetsadobe2.com/is/image/content/dam/dyson/in/vacuum-cleaners/v12"),

                product("Himalayan Pink Salt Lamp",
                        "Natural hand-carved Himalayan crystal salt lamp on a neem wood base. " +
                                "Warm amber glow, 2.5–3kg crystal. Includes dimmer switch and 15W bulb.",
                        new BigDecimal("1299.00"), 200, homeKitchen,
                        "https://images.unsplash.com/photo-1544947950-fa07a98d237f?w=400")
        );

        // ── Persist to MySQL ────────────────────────────────────────────────
        List<Product> saved = productRepository.saveAll(products);
        log.info("Saved {} products to MySQL", saved.size());

        // ── Index in Elasticsearch ──────────────────────────────────────────
        List<ProductDocument> docs = saved.stream()
                .map(p -> ProductDocument.builder()
                        .id(p.getId().toString())
                        .name(p.getName())
                        .description(p.getDescription())
                        .category(p.getCategory() != null ? p.getCategory().getName() : null)
                        .price(p.getPrice())
                        .state(p.getState().name())
                        .imageUrl(p.getImageUrl())
                        .build())
                .toList();

        searchRepository.saveAll(docs);
        log.info("Indexed {} products in Elasticsearch", docs.size());
        log.info("──────────────────────────────────────────────────────────────");
        log.info("  Product seeding complete: {} categories, {} products", 4, saved.size());
        log.info("──────────────────────────────────────────────────────────────");
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private Category getOrCreateCategory(String name, String description) {
        return categoryRepository.findByName(name).orElseGet(() -> {
            Category c = Category.builder().name(name).description(description).build();
            return categoryRepository.save(c);
        });
    }

    private Product product(String name, String description, BigDecimal price,
                            int stock, Category category, String imageUrl) {
        return Product.builder()
                .name(name)
                .description(description)
                .price(price)
                .stockQuantity(stock)
                .category(category)
                .imageUrl(imageUrl)
                .build();
    }
}