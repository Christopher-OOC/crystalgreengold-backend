package com.topnivo.backend.service;

import com.topnivo.backend.exception.exception.BadRequestException;
import com.topnivo.backend.exception.exception.ErrorMessages;
import com.topnivo.backend.exception.exception.InsufficientProductQuantity;
import com.topnivo.backend.exception.exception.NoSuchResourceException;
import com.topnivo.backend.model.entity.*;
import com.topnivo.backend.model.request.CartItemUpdateRequest;
import com.topnivo.backend.model.request.CartItemCreateRequest;
import com.topnivo.backend.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class CartService {

    private final CartRepository cartRepository;
    private final MemberService memberService;
    private final ProductService productService;
    private final CartItemService cartItemService;
    private final MemberRepository memberRepository;
    private final CartItemRepository cartItemRepository;
    private final StoreProductRepository storeProductRepository;
    private final ProductRepository productRepository;

    public Cart addProductToCart(String memberId, CartItemCreateRequest request) {
        // Validate and fetch required entities
        Member member = validateAndGetMember(memberId);
        Product product = validateAndGetProduct(request.getProductId());
        Member store = getStoreIfApplicable(request.getStoreId());

        // Handle new cart item based on user roles and store type
        UserRoleType userRoleType = determineUserRoleType();

        if (!product.isDiscounted()) {
            validatePurchasePermissions(userRoleType, store);
        }

        Cart membercart = cartRepository.findByMember(member);

        if (membercart != null) {
            if (product.isDiscounted()) {
                for (CartItem cartItem : membercart.getCartItems()) {
                    if (!cartItem.getProduct().isDiscounted()) {
                        membercart.getCartItems().remove(cartItem);
                    }
                }

                membercart = cartRepository.save(membercart);
            }

            Optional<CartItem> existingCartItem = membercart
                    .getCartItems()
                    .stream()
                    .filter(cartItem -> Objects.equals(cartItem.getProduct().getId(), product.getId()))
                    .findFirst();

            if (existingCartItem.isPresent()) {
                return handleExistingCartItem(member, product, request.getQuantity(), existingCartItem.get());
            }
            else {
                return addNewProductToCart(member, store, product, request.getQuantity());
            }
        }
        else {
            return addNewProductToCart(member, store, product, request.getQuantity());
        }
    }

    private Member validateAndGetMember(String memberId) {
        Member member = memberService.findMemberByMemberId(memberId);
        if (member == null) {
            throw new NoSuchResourceException(ErrorMessages.NO_SUCH_MEMBER);
        }
        return member;
    }

    private Product validateAndGetProduct(String productId) {
        Product product = productService.findProductById(productId);
        if (product == null) {
            throw new NoSuchResourceException(ErrorMessages.NO_SUCH_PRODUCT);
        }
        return product;
    }

    private Member getStoreIfApplicable(String storeId) {
        return storeId != null ? memberService.findMemberByMemberId(storeId) : null;
    }

    private UserRoleType determineUserRoleType() {
        var authorities = SecurityContextHolder.getContext().getAuthentication().getAuthorities();

        String role = authorities.stream()
                .map(GrantedAuthority::getAuthority)
                .filter(r -> !r.equals("ROLE_REGULAR_MEMBER"))
                .findFirst()
                .orElse("ROLE_REGULAR_MEMBER");

        return UserRoleType.fromString(role);
    }

    private void validatePurchasePermissions(UserRoleType userRoleType, Member store) {
        if (userRoleType == UserRoleType.REGULAR_MEMBER) {
            if (store == null || !storeHasRole(store, "ROLE_SERVICE_CENTER")) {
                throw new BadRequestException(ErrorMessages.BUY_ONLY_FROM_SERVICE_CENTER);
            }
        } else if (userRoleType == UserRoleType.SERVICE_CENTER) {
            if (store == null || !storeHasRole(store, "ROLE_PREMIUM_STORE")) {
                throw new BadRequestException(ErrorMessages.BUY_ONLY_FROM_PREMIUM_STORE);
            }
        }
    }

    private boolean storeHasRole(Member store, String roleName) {
        return store.getRoles().stream().anyMatch(r -> r.getName().equals(roleName));
    }

    private Cart handleExistingCartItem(Member member, Product product, int requestedQuantity, CartItem existingCartItem) {
        validateProductQuantity(product, requestedQuantity + existingCartItem.getQuantity());

        existingCartItem.setQuantity(existingCartItem.getQuantity() + requestedQuantity);
        cartItemRepository.save(existingCartItem);

        return cartRepository.findById(member.getCart().getId());
    }

    private Cart addNewProductToCart(Member member, Member store, Product product, int quantity) {
        if (store == null) {
            if (determineUserRoleType() == UserRoleType.PREMIUM_STORE || product.isDiscounted()) {
                validateProductQuantity(product, quantity);
                return createCartItem(member, null, product, quantity);
            }
            else {
                throw new BadRequestException(ErrorMessages.INVALID_OPERATION);
            }
        }
        else {
            // Don't allow to member to add product from different store
            checkIfProductIsAlreadyInCart(member, store);

            // For store purchases
            String storeRole = store.getRoles().stream()
                    .map(Role::getName)
                    .filter(r -> !r.equals("ROLE_REGULAR_MEMBER"))
                    .findFirst()
                    .orElse("ROLE_REGULAR_MEMBER");
            if ((determineUserRoleType() == UserRoleType.REGULAR_MEMBER && storeRole.equals("ROLE_SERVICE_CENTER")) ||
                    (determineUserRoleType() == UserRoleType.SERVICE_CENTER && storeRole.equals("ROLE_PREMIUM_STORE"))
            ) {
                StoreProduct storeProduct = storeProductRepository.findProductFromStore(store.getMemberId(), product.getId());
                if (storeProduct == null) {
                    throw new NoSuchResourceException(ErrorMessages.NO_SUCH_PRODUCT);
                }
                validateProductQuantity(storeProduct, quantity);

                return createCartItem(member, store, product, quantity);            }
            else {
                throw new BadRequestException(ErrorMessages.INVALID_OPERATION);
            }
        }
    }

    private void checkIfProductIsAlreadyInCart(Member member, Member store) {
        Cart cart = member.getCart();
        if (cart == null) {
            return;
        }
        List<CartItem> cartItems = cart.getCartItems();
        for (CartItem cartItem : cartItems) {
            if (cartItem.getStore().getId() != store.getId()) {
                throw new BadRequestException(ErrorMessages.BUY_PRODUCTS_ONLY_FRON_THE_SAME_STORE);
            }
        }
    }

    private void validateProductQuantity(Product product, int requiredQuantity) {
        if (product.getAvailableQuantity() < requiredQuantity) {
            throw new InsufficientProductQuantity(ErrorMessages.INSUFFICIENT_PRODUCT_IN_STOCK);
        }
    }

    private void validateProductQuantity(StoreProduct storeProduct, int requiredQuantity) {
        if (storeProduct.getBoughtQuantity() < requiredQuantity) {
            throw new InsufficientProductQuantity(ErrorMessages.INSUFFICIENT_PRODUCT_IN_STOCK);
        }
    }

    private Cart createCartItem(Member member, Member store, Product product, int quantity) {
        CartItem newCartItem = new CartItem();
        newCartItem.setProduct(product);
        newCartItem.setQuantity(quantity);
        newCartItem.setStore(store);

        if (member.getCart() == null) {
            Cart cart = new Cart();
            cart.setMember(member);
            cart.getCartItems().add(newCartItem);
            cart = cartRepository.save(cart);
            newCartItem.setCart(cart);
            member.setCart(cart);
        } else {
            newCartItem.setCart(member.getCart());
            member.getCart().getCartItems().add(newCartItem);
            cartItemRepository.save(newCartItem);
        }

        return memberRepository.save(member).getCart();
    }

    // Enum for better role handling
    public enum UserRoleType {
        REGULAR_MEMBER,
        SERVICE_CENTER,
        PREMIUM_STORE,
        ADMIN,
        SUPER_ADMIN,
        UNKNOWN;

        public static UserRoleType fromString(String role) {
            return switch (role) {
                case "ROLE_REGULAR_MEMBER" -> REGULAR_MEMBER;
                case "ROLE_SERVICE_CENTER" -> SERVICE_CENTER;
                case "ROLE_PREMIUM_STORE" -> PREMIUM_STORE;
                case "ROLE_ADMIN" -> ADMIN;
                case "ROLE_SUPER_ADMIN" -> SUPER_ADMIN;
                default -> UNKNOWN;
            };
        }
    }

    public Cart findCartByMemberId(String memberId) {
        Member member = memberService.findMemberByMemberId(memberId);
        if (Objects.isNull(member)) {
            throw new NoSuchResourceException(ErrorMessages.NO_SUCH_MEMBER);
        }

        return member.getCart();
    }

    public Cart updateCartItemQuantity(String memberId, List<CartItemUpdateRequest> request) {
        Cart cart = findCartByMemberId(memberId);
        if (!Objects.isNull(cart)) {
            // Sorted request
            request.sort(Comparator.comparingInt(CartItemUpdateRequest::getCartItemId));
            // Sorted memberCartItems
            List<CartItem> memberCartItems = cart.getCartItems();
            memberCartItems.sort(Comparator.comparingInt(CartItem::getId));
            if (memberCartItems.size() != request.size()) {
                throw new BadRequestException(ErrorMessages.INVALID_CART_ITEM);
            }
            List<Integer> memberCartItemsIds = memberCartItems.stream().map(CartItem::getId).toList();
            List<Integer> requestCartItemsIds = request.stream().map(CartItemUpdateRequest::getCartItemId).toList();
            boolean isAllCartRequestPresentInCart = new HashSet<>(memberCartItemsIds).containsAll(requestCartItemsIds);
            if (!isAllCartRequestPresentInCart) {
                throw new BadRequestException(ErrorMessages.INVALID_CART_ITEM);
            }

            for (int i = 0; i < memberCartItems.size(); i++) {
                if (memberCartItems.get(i).getProduct().getAvailableQuantity() < request.get(i).getQuantity()) {
                    throw new InsufficientProductQuantity(ErrorMessages.INSUFFICIENT_PRODUCT_IN_STOCK);
                }
                memberCartItems.get(i).setQuantity(request.get(i).getQuantity());
            }

            cartItemRepository.saveAll(memberCartItems);

            return cartRepository.findById(cart.getId());
        }
        else {
            throw new NoSuchResourceException(ErrorMessages.NO_SUCH_CART);
        }
    }

    public Cart deleteCartItemFromCart(String memberId, int cartItemId) {
        Cart cart = findCartByMemberId(memberId);
        CartItem cartItem = cartItemService.findById(cartItemId);
        cart.getCartItems().remove(cartItem);
        cartItemRepository.delete(cartItem);
        return cartRepository.save(cart);
    }
}
