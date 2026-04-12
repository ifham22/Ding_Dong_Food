// Page Navigation System
let pageHistory = [];
let currentPage = 'landing-page';
let cart = [];
let currentUser = null;
const API_BASE = window.location.protocol === 'file:' ? 'http://localhost:8081' : window.location.origin;

function apiUrl(path) {
    return API_BASE + path;
}

// Initialize
document.addEventListener('DOMContentLoaded', () => {
    console.log('DOM Content Loaded - Setting up event listeners...');
    
    try {
        const registerForm = document.getElementById('register-user-form');
        if (registerForm) {
            registerForm.addEventListener('submit', handleRegister);
            console.log('Register form listener attached');
        }
    } catch (e) { console.error('Error attaching register listener:', e); }
    
    try {
        const loginForm = document.getElementById('login-user-form');
        if (loginForm) {
            loginForm.addEventListener('submit', handleLogin);
            console.log('Login form listener attached');
        }
    } catch (e) { console.error('Error attaching login listener:', e); }
    
    try {
        const regRestForm = document.getElementById('register-restaurant-form');
        if (regRestForm) {
            regRestForm.addEventListener('submit', handleRestaurantRegister);
            console.log('Register restaurant form listener attached');
        }
    } catch (e) { console.error('Error attaching register restaurant listener:', e); }
    
    try {
        const restLoginForm = document.getElementById('restaurant-login-form');
        if (restLoginForm) {
            restLoginForm.addEventListener('submit', handleRestaurantLogin);
            console.log('Restaurant login form listener attached');
        }
    } catch (e) { console.error('Error attaching restaurant login listener:', e); }
    
    try {
        const restAddMenuForm = document.getElementById('restaurant-add-menu-form');
        if (restAddMenuForm) {
            restAddMenuForm.addEventListener('submit', handleRestaurantAddMenu);
            console.log('Restaurant add menu form listener attached');
        }
    } catch (e) { console.error('Error attaching restaurant add menu listener:', e); }
    
    try {
        const addMenuForm = document.getElementById('add-menu-form');
        if (addMenuForm) {
            addMenuForm.addEventListener('submit', addMenuItem);
            console.log('Add menu form listener attached');
        }
    } catch (e) { console.error('Error attaching add menu listener:', e); }
    
    try {
        const schedForm = document.getElementById('schedule-form');
        if (schedForm) {
            schedForm.addEventListener('submit', scheduleOrder);
            console.log('Schedule form listener attached');
        }
    } catch (e) { console.error('Error attaching schedule listener:', e); }
});

// PAGE NAVIGATION LOGIC
function goToPage(pageId) {
    // Hide all pages
    const pages = document.querySelectorAll('.page');
    pages.forEach(page => page.style.display = 'none');

    // Stop order refresh when leaving order-tracking page
    if (currentPage === 'order-tracking' && orderRefreshInterval) {
        clearInterval(orderRefreshInterval);
        orderRefreshInterval = null;
    }

    // Track history
    pageHistory.push(currentPage);
    currentPage = pageId;

    // Show the requested page
    const page = document.getElementById(pageId);
    if (page) {
        page.style.display = 'block';
        
        // Load page-specific data
        if (pageId === 'main-app') {
            fetchRestaurants();
            updateUserStatus();
        }
        if (pageId === 'checkout') {
            updateCartDisplay();
        }
        if (pageId === 'user-profile') {
            loadUserProfile();
        }
    }
}

function goBack() {
    if (pageHistory.length > 0) {
        const pages = document.querySelectorAll('.page');
        pages.forEach(page => page.style.display = 'none');
        
        currentPage = pageHistory.pop();
        const page = document.getElementById(currentPage);
        if (page) {
            page.style.display = 'block';
        }
    }
}

// REGISTRATION HANDLER
function handleRegister(event) {
    event.preventDefault();
    console.log('Registration form submitted');
    
    const formData = new FormData(event.target);
    const name = formData.get('name');
    const email = formData.get('email');
    const password = formData.get('password');
    const address = formData.get('address') || '';

    console.log('Register data:', { name, email, address });

    const data = new URLSearchParams();
    data.append('name', name);
    data.append('email', email);
    data.append('password', password);
    data.append('address', address);

    console.log('Sending registration request to:', apiUrl('/api/register-user'));

    fetch(apiUrl('/api/register-user'), {
        method: 'POST',
        headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
        body: data
    })
    .then(response => {
        console.log('Response status:', response.status);
        if (!response.ok) {
            return response.text().then(text => {
                console.error('Server error response:', text);
                throw new Error(`HTTP ${response.status}: ${text}`);
            });
        }
        return response.json();
    })
    .then(res => {
        console.log('Registration response:', res);
        if (res.message) {
            alert('Registration successful! You can now login.');
            event.target.reset();
            goToPage('login');
        } else if (res.error) {
            alert('Error: ' + res.error);
        } else {
            alert('Registration completed!');
            event.target.reset();
            goToPage('login');
        }
    })
    .catch(err => {
        console.error('Registration error:', err);
        alert('Error: ' + err.message);
    });
}

// LOGIN HANDLER
function handleLogin(event) {
    event.preventDefault();
    console.log('Login form submitted');
    
    const formData = new FormData(event.target);
    const data = new URLSearchParams(formData);
    
    console.log('Sending login request to:', apiUrl('/api/login'));
    
    fetch(apiUrl('/api/login'), {
        method: 'POST',
        headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
        body: data
    })
    .then(response => {
        console.log('Login response status:', response.status);
        if (!response.ok && response.status !== 401) {
            return response.text().then(text => {
                console.error('Server error response:', text);
                throw new Error(`HTTP ${response.status}: ${text}`);
            });
        }
        return response.json();
    })
    .then(res => {
        console.log('Login response:', res);
        if (res.user) {
            currentUser = res.user;
            alert('Login successful!');
            event.target.reset();
            goToPage('main-app');
        } else if (res.error) {
            alert('Error: ' + res.error);
        } else {
            alert('Login failed');
        }
    })
    .catch(err => {
        console.error('Login error:', err);
        alert('Error: ' + err.message);
    });
}

// LOGOUT HANDLER
function logoutUser() {
    currentUser = null;
    cart = [];
    alert('You have been logged out.');
    pageHistory = [];
    const pages = document.querySelectorAll('.page');
    pages.forEach(page => page.style.display = 'none');
    document.getElementById('landing-page').style.display = 'block';
    currentPage = 'landing-page';
}

// USER STATUS UPDATE
function updateUserStatus() {
    if (currentUser) {
        document.getElementById('current-user-name').textContent = currentUser.name;
    }
}

// RESTAURANT FUNCTIONS
function fetchRestaurants(url = apiUrl('/api/restaurants')) {
    const container = document.getElementById('restaurant-container');
    const status = document.getElementById('status');
    
    if (!container) return;
    
    status.innerText = 'Refreshing...';
    
    fetch(url)
        .then(response => {
            if (!response.ok) throw new Error('Network response was not ok');
            return response.json();
        })
        .then(data => {
            container.innerHTML = '';
            
            if(data.error) {
                container.innerHTML = `<div class="error-msg">Backend Error: ${data.error}</div>`;
                status.innerText = 'Error';
                return;
            }
            
            if (data.length === 0) {
                container.innerHTML = `<p>No restaurants found in database.</p>`;
                status.innerText = '0 Results';
                return;
            }
            
            data.forEach(rest => {
                const card = document.createElement('div');
                card.className = 'restaurant-card';
                card.innerHTML = `
                    <img src="${rest.imageUrl || 'https://via.placeholder.com/400x300?text=No+Image'}" alt="${rest.name}" class="card-img">
                    <div class="card-content">
                        <span class="badge">${rest.cuisineType || 'Unknown'}</span>
                        <h3>${rest.name}</h3>
                        <p class="address">📍 ${rest.address || 'No address'}</p>
                        <button class="btn-secondary" onclick="viewMenu(${rest.id}, '${rest.name}')">View Menu</button>
                    </div>
                `;
                container.appendChild(card);
            });
            
            status.innerText = `${data.length} Results`;
            populateSelect(data);
        })
        .catch(error => {
            console.error('Error fetching restaurants:', error);
            container.innerHTML = `<div class="error-msg">Failed to connect to backend server. Make sure Java is running!</div>`;
            status.innerText = 'Connection Failed';
        });
}

function filterByLocation() {
    const location = document.getElementById('location-filter').value;
    if (location === '') {
        fetchRestaurants();
    } else {
        fetchRestaurants(apiUrl('/api/search?q=' + encodeURIComponent(location)));
    }
}

function populateSelect(restaurants) {
    const select = document.getElementById('restaurant-select');
    if (!select) return;
    select.innerHTML = '<option value="">Select Restaurant</option>';
    restaurants.forEach(r => {
        const option = document.createElement('option');
        option.value = r.id;
        option.textContent = r.name;
        select.appendChild(option);
    });
}

function registerRestaurant(e) {
    e.preventDefault();
    const formData = new FormData(e.target);
    const data = new URLSearchParams(formData);
    fetch(apiUrl('/api/register-restaurant'), {
        method: 'POST',
        headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
        body: data
    })
    .then(r => r.json())
    .then(res => {
        alert(res.message || res.error);
        if (res.message) {
            fetchRestaurants();
        }
    })
    .catch(err => alert('Error: ' + err));
}

function viewMenu(restId, restName) {
    // Set currentRestaurant when viewing a restaurant's menu
    currentRestaurant = { id: restId, name: restName };
    console.log('Selected restaurant:', currentRestaurant);
    
    fetch(apiUrl('/api/menu?restId=' + restId))
        .then(r => r.json())
        .then(data => {
            const container = document.getElementById('menu-items-container');
            document.getElementById('menu-restaurant-name').textContent = restName + ' - Menu';
            container.innerHTML = '';
            
            if (data.length === 0) {
                container.innerHTML = '<p>No menu items available.</p>';
            } else {
                const table = document.createElement('table');
                table.style.width = '100%';
                table.style.borderCollapse = 'collapse';
                table.innerHTML = '<tr style="background: #f0f0f0; border-bottom: 1px solid #ccc;"><th style="padding: 10px; text-align: left;">Item (Category)</th><th style="padding: 10px; text-align: right;">Price</th><th style="padding: 10px; text-align: center;">Quantity</th><th style="padding: 10px; text-align: center;">Action</th></tr>';
                
                data.forEach(item => {
                    const row = table.insertRow();
                    row.style.borderBottom = '1px solid #eee';
                    
                    // Item & Description column
                    const itemCell = row.insertCell();
                    itemCell.innerHTML = `<strong>${item.name}</strong><br><small style="color: #666;">${item.description}</small><br><small style="color: #999;">${item.category}</small>`;
                    itemCell.style.padding = '10px';
                    
                    // Price column
                    const priceCell = row.insertCell();
                    priceCell.innerHTML = `<strong>Tk${item.price}</strong>`;
                    priceCell.style.padding = '10px';
                    priceCell.style.textAlign = 'right';
                    
                    // Quantity input column
                    const qtyCell = row.insertCell();
                    const qtyInput = document.createElement('input');
                    qtyInput.type = 'number';
                    qtyInput.min = '1';
                    qtyInput.max = '10';
                    qtyInput.value = '1';
                    qtyInput.style.width = '60px';
                    qtyInput.style.padding = '5px';
                    qtyCell.appendChild(qtyInput);
                    qtyCell.style.padding = '10px';
                    qtyCell.style.textAlign = 'center';
                    
                    // Action column
                    const actionCell = row.insertCell();
                    actionCell.style.padding = '10px';
                    actionCell.style.textAlign = 'center';
                    
                    // If pizza, add size selector
                    if (item.hasSizes) {
                        const sizeSelect = document.createElement('select');
                        sizeSelect.style.marginRight = '5px';
                        sizeSelect.style.padding = '5px';
                        const sizes = item.sizes.split('|');
                        sizes.forEach(size => {
                            const option = document.createElement('option');
                            option.value = size;
                            option.textContent = size;
                            sizeSelect.appendChild(option);
                        });
                        actionCell.appendChild(sizeSelect);
                        
                        const btn = document.createElement('button');
                        btn.textContent = 'Add';
                        btn.style.background = '#3c90ff';
                        btn.style.color = 'white';
                        btn.style.border = 'none';
                        btn.style.padding = '5px 10px';
                        btn.style.borderRadius = '3px';
                        btn.style.cursor = 'pointer';
                        btn.onclick = () => addToCart(restId, item.id, item.price, item.name, parseInt(qtyInput.value), sizeSelect.value);
                        actionCell.appendChild(btn);
                    } else {
                        const btn = document.createElement('button');
                        btn.textContent = 'Add';
                        btn.style.background = '#3c90ff';
                        btn.style.color = 'white';
                        btn.style.border = 'none';
                        btn.style.padding = '5px 10px';
                        btn.style.borderRadius = '3px';
                        btn.style.cursor = 'pointer';
                        btn.onclick = () => addToCart(restId, item.id, item.price, item.name, parseInt(qtyInput.value), null);
                        actionCell.appendChild(btn);
                    }
                });
                container.appendChild(table);
            }
            
            document.getElementById('menu-modal').style.display = 'block';
        })
        .catch(err => alert('Error fetching menu: ' + err));
}

function closeMenuModal() {
    document.getElementById('menu-modal').style.display = 'none';
}

function addToCart(restId, menuId, basePrice, name, quantity, size) {
    // Ensure currentRestaurant is set
    if (!currentRestaurant || currentRestaurant.id !== restId) {
        // Fallback: set from cart or parameter
        currentRestaurant = { id: restId };
    }
    
    if (cart.length > 0 && cart[0].restId !== restId) {
        alert('You can only order from one restaurant at a time. Please clear the cart first.');
        return;
    }

    quantity = quantity || 1;
    
    // Calculate price based on size
    let price = basePrice;
    if (size) {
        if (size === 'Medium') price = basePrice + 50; // +50 for medium
        else if (size === 'Large') price = basePrice + 100; // +100 for large
    }
    
    // Create unique key for tracking item variants (same item with different sizes)
    const itemKey = `${menuId}_${size || 'standard'}`;
    
    const item = cart.find(i => i.menuId === menuId && i.size === (size || null));
    if (!item) {
        cart.push({ restId, menuId, quantity, price, name, size: size || null });
    } else {
        item.quantity += quantity;
    }
    alert(`Added ${quantity}x ${name}${size ? ' (' + size + ')' : ''} to cart!`);
    updateCartDisplay();
}

function updateCartDisplay() {
    const totalItems = cart.reduce((sum, i) => sum + i.quantity, 0);
    const subtotal = cart.reduce((sum, i) => sum + i.quantity * i.price, 0);
    const finalTotal = (subtotal - currentDiscount).toFixed(2);
    
    const cartItemsDiv = document.getElementById('cart-items');
    const cartSubtotalDiv = document.getElementById('cart-subtotal');
    const cartDiscountDiv = document.getElementById('cart-discount');
    const cartTotalDiv = document.getElementById('cart-total');
    
    if (!cartItemsDiv) return;
    
    if (cart.length === 0) {
        cartItemsDiv.innerHTML = '<p>Your cart is empty.</p>';
        if (cartTotalDiv) cartTotalDiv.textContent = 'Tk 0.00';
        if (cartSubtotalDiv) cartSubtotalDiv.textContent = 'Tk 0.00';
        if (cartDiscountDiv) cartDiscountDiv.textContent = '- Tk 0.00';
        return;
    }

    let html = '<table style="width:100%; border-collapse: collapse; margin-top: 10px;">';
    html += '<tr style="background: #f0f0f0;"><th style="padding: 10px; text-align: left;">Item</th><th style="padding: 10px; text-align: center;">Qty</th><th style="padding: 10px; text-align: right;">Price</th><th style="padding: 10px; text-align: right;">Total</th><th style="padding: 10px; text-align: center;">Actions</th></tr>';

    cart.forEach((item, index) => {
        html += `<tr style="border-bottom: 1px solid #eee;">`;
        html += `<td style="padding: 10px;">${item.name}${item.size ? ' (' + item.size + ')' : ''}</td>`;
        html += `<td style="padding: 10px; text-align: center;">${item.quantity}</td>`;
        html += `<td style="padding: 10px; text-align: right;">Tk ${item.price.toFixed(2)}</td>`;
        html += `<td style="padding: 10px; text-align: right;">Tk ${(item.quantity * item.price).toFixed(2)}</td>`;
        html += `<td style="padding: 10px; text-align: center;">`;
        html += `<button onclick="changeQuantity(${index}, -1)" style="margin-right: 5px; padding: 5px 10px;">-</button>`;
        html += `<button onclick="changeQuantity(${index}, 1)" style="margin-right: 5px; padding: 5px 10px;">+</button>`;
        html += `<button onclick="removeFromCart(${index})" style="background: #ff6b6b; color: white; padding: 5px 10px; border: none; border-radius: 3px; cursor: pointer;">Remove</button>`;
        html += `</td>`;
        html += `</tr>`;
    });
    html += '</table>';

    cartItemsDiv.innerHTML = html;
    if (cartSubtotalDiv) cartSubtotalDiv.textContent = 'Tk ' + subtotal.toFixed(2);
    if (cartDiscountDiv) cartDiscountDiv.textContent = '- Tk ' + currentDiscount.toFixed(2);
    if (cartTotalDiv) cartTotalDiv.textContent = 'Tk ' + finalTotal;
}

function removeFromCart(index) {
    cart.splice(index, 1);
    updateCartDisplay();
}

function changeQuantity(index, delta) {
    const item = cart[index];
    if (!item) return;
    item.quantity += delta;
    if (item.quantity <= 0) {
        cart.splice(index, 1);
    }
    updateCartDisplay();
}

function clearCart() {
    cart = [];
    currentDiscount = 0;
    appliedCoupon = null;
    updateCartDisplay();
}

function checkoutOrder() {
    if (cart.length === 0) {
        alert('Your cart is empty!');
        return;
    }

    if (!currentUser) {
        alert('Please login first');
        return;
    }

    if (!currentRestaurant) {
        alert('No restaurant selected');
        return;
    }

    const paymentMethodRadio = document.querySelector('input[name="payment-method"]:checked');
    if (!paymentMethodRadio) {
        alert('Please select a payment method');
        return;
    }

    const paymentMethod = paymentMethodRadio.value;
    const subtotal = cart.reduce((sum, item) => sum + (item.price * item.quantity), 0);
    const finalTotal = subtotal - currentDiscount;

    console.log('Processing payment:', { paymentMethod, subtotal, currentDiscount, finalTotal, restaurantId: currentRestaurant.id });

    // Prepare cart items as JSON
    const cartItems = cart.map(item => ({
        id: item.id,
        name: item.name,
        price: item.price,
        quantity: item.quantity
    }));

    // Process payment
    const data = new URLSearchParams();
    data.append('userId', currentUser.id);
    data.append('restaurantId', currentRestaurant.id);
    data.append('paymentMethod', paymentMethod);
    data.append('subtotal', subtotal);
    data.append('discount', currentDiscount);
    data.append('amount', finalTotal);
    data.append('coupon', appliedCoupon || '');
    data.append('cartItems', JSON.stringify(cartItems));

    console.log('Sending data:', data.toString());

    fetch(apiUrl('/api/payment'), {
        method: 'POST',
        headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
        body: data.toString()
    })
    .then(r => {
        console.log('Response status:', r.status);
        if (!r.ok) {
            return r.text().then(text => {
                console.error('Server error response:', text);
                throw new Error(`Server error (${r.status}): ${text}`);
            });
        }
        return r.json();
    })
    .then(payment => {
        console.log('Payment response:', payment);
        if (payment.status === 'COMPLETED' || payment.orderId) {
            alert(`✓ Order placed successfully!\nOrder ID: ${payment.orderId}\nPayment Method: ${paymentMethod}\nAmount: Tk ${finalTotal.toFixed(2)}\nTransaction ID: ${payment.transactionId}`);
            clearCart();
            document.getElementById('coupon-input').value = '';
            document.getElementById('coupon-input').disabled = false;
            document.getElementById('coupon-message').innerHTML = '';
            goToPage('main-app');
        } else {
            alert('✗ Payment failed. Status: ' + payment.status);
        }
    })
    .catch(err => {
        console.error('Payment error:', err);
        alert('Error processing payment:\n' + err.message);
    });
}

function addMenuItem(e) {
    e.preventDefault();
    const formData = new FormData(e.target);
    const data = new URLSearchParams(formData);
    fetch(apiUrl('/api/add-menu'), {
        method: 'POST',
        headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
        body: data
    })
    .then(r => r.json())
    .then(res => {
        alert(res.message || res.error);
        e.target.reset();
    })
    .catch(err => alert('Error: ' + err));
}

function scheduleOrder(e) {
    e.preventDefault();
    const orderId = document.getElementById('schedule-order-id').value;
    const deliveryTime = document.getElementById('schedule-time').value;

    if (!orderId || !deliveryTime) {
        alert('Please provide order ID and delivery time.');
        return;
    }

    fetch(apiUrl('/api/schedule-order'), {
        method: 'POST',
        headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
        body: `orderId=${encodeURIComponent(orderId)}&deliveryTime=${encodeURIComponent(deliveryTime)}`
    })
    .then(r => r.json())
    .then(res => {
        alert(res.message || res.error);
        if (res.message) {
            e.target.reset();
        }
    })
    .catch(err => alert('Error: ' + err));
}

// ============== RESTAURANT FUNCTIONS ==============

var currentRestaurant = null;

function handleRestaurantRegister(event) {
    event.preventDefault();
    console.log('Restaurant registration form submitted');
    
    const formData = new FormData(event.target);
    const name = formData.get('name');
    const address = formData.get('address');
    const cuisineType = formData.get('cuisineType');
    const password = formData.get('password');

    console.log('Register restaurant data:', { name, address, cuisineType });

    const data = new URLSearchParams();
    data.append('name', name);
    data.append('address', address);
    data.append('cuisineType', cuisineType);
    data.append('password', password);

    fetch(apiUrl('/api/register-restaurant'), {
        method: 'POST',
        headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
        body: data
    })
    .then(response => {
        console.log('Response status:', response.status);
        if (!response.ok) {
            return response.text().then(text => {
                console.error('Server error response:', text);
                throw new Error(`HTTP ${response.status}: ${text}`);
            });
        }
        return response.json();
    })
    .then(res => {
        console.log('Registration response:', res);
        if (res.message) {
            alert('Restaurant registered successfully! You can now login.');
            event.target.reset();
            goToPage('restaurant-login');
        } else if (res.error) {
            alert('Error: ' + res.error);
        }
    })
    .catch(err => {
        console.error('Registration error:', err);
        alert('Error: ' + err.message);
    });
}

function handleRestaurantLogin(event) {
    event.preventDefault();
    console.log('Restaurant login form submitted');
    
    const formData = new FormData(event.target);
    const data = new URLSearchParams(formData);
    
    console.log('Sending restaurant login request to:', apiUrl('/api/restaurant-login'));
    
    fetch(apiUrl('/api/restaurant-login'), {
        method: 'POST',
        headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
        body: data
    })
    .then(response => {
        console.log('Login response status:', response.status);
        if (!response.ok && response.status !== 401) {
            return response.text().then(text => {
                console.error('Server error response:', text);
                throw new Error(`HTTP ${response.status}: ${text}`);
            });
        }
        return response.json();
    })
    .then(res => {
        console.log('Login response:', res);
        if (res.restaurant) {
            currentRestaurant = res.restaurant;
            console.log('Login successful for restaurant:', currentRestaurant.name);
            alert('Login successful!');
            event.target.reset();
            updateRestaurantStatus();
            loadRestaurantMenu();
            goToPage('restaurant-dashboard');
        } else if (res.error) {
            alert('Error: ' + res.error);
        }
    })
    .catch(err => {
        console.error('Login error:', err);
        alert('Error: ' + err.message);
    });
}

function logoutRestaurant() {
    currentRestaurant = null;
    alert('You have been logged out.');
    pageHistory = [];
    const pages = document.querySelectorAll('.page');
    pages.forEach(page => page.style.display = 'none');
    document.getElementById('landing-page').style.display = 'block';
    currentPage = 'landing-page';
}

function updateRestaurantStatus() {
    if (currentRestaurant) {
        document.getElementById('current-restaurant-name').textContent = 'Welcome, ' + currentRestaurant.name + '!';
    }
}

function handleRestaurantAddMenu(event) {
    event.preventDefault();
    console.log('Restaurant add menu form submitted');
    
    if (!currentRestaurant) {
        alert('You must be logged in as a restaurant');
        return;
    }
    
    const formData = new FormData(event.target);
    const data = new URLSearchParams();
    data.append('restId', currentRestaurant.id);
    data.append('name', formData.get('name'));
    data.append('description', formData.get('description'));
    data.append('price', formData.get('price'));
    data.append('qty', formData.get('qty'));

    fetch(apiUrl('/api/add-menu'), {
        method: 'POST',
        headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
        body: data
    })
    .then(response => {
        if (!response.ok) {
            return response.text().then(text => {
                throw new Error(`HTTP ${response.status}: ${text}`);
            });
        }
        return response.json();
    })
    .then(res => {
        if (res.message) {
            alert('Menu item added successfully!');
            event.target.reset();
            loadRestaurantMenu();
        } else {
            alert(res.error || 'Failed to add menu item');
        }
    })
    .catch(err => {
        console.error('Add menu error:', err);
        alert('Error: ' + err.message);
    });
}

function loadRestaurantMenu() {
    if (!currentRestaurant) return;
    
    fetch(apiUrl('/api/menu?restId=' + currentRestaurant.id))
        .then(r => r.json())
        .then(data => {
            const container = document.getElementById('restaurant-menu-list');
            
            if (data.length === 0) {
                container.innerHTML = '<p>No menu items yet. Add one above!</p>';
                return;
            }

            let html = '<table style="width: 100%; border-collapse: collapse;">';
            html += '<tr style="background: #f0f0f0; border-bottom: 2px solid #ddd;"><th style="padding: 10px; text-align: left;">Item Name</th><th style="padding: 10px; text-align: left;">Description</th><th style="padding: 10px; text-align: right;">Price</th><th style="padding: 10px; text-align: right;">Qty</th><th style="padding: 10px; text-align: center;">Actions</th></tr>';

            data.forEach(item => {
                html += `<tr style="border-bottom: 1px solid #eee;">`;
                html += `<td style="padding: 10px;">${item.name}</td>`;
                html += `<td style="padding: 10px;">${item.description}</td>`;
                html += `<td style="padding: 10px; text-align: right;">Tk ${item.price.toFixed(2)}</td>`;
                html += `<td style="padding: 10px; text-align: right;">${item.quantity}</td>`;
                html += `<td style="padding: 10px; text-align: center;">`;
                html += `<button onclick="deleteRestaurantMenuItem(${item.id})" style="background: #ff6b6b; color: white; border: none; padding: 5px 10px; border-radius: 3px; cursor: pointer;">Delete</button>`;
                html += `</td>`;
                html += `</tr>`;
            });
            html += '</table>';

            container.innerHTML = html;
        })
        .catch(err => {
            console.error('Error loading menu:', err);
            document.getElementById('restaurant-menu-list').innerHTML = '<p style="color: red;">Error loading menu items</p>';
        });
}

function deleteRestaurantMenuItem(menuId) {
    if (confirm('Are you sure you want to delete this menu item?')) {
        fetch(apiUrl('/api/delete-menu?menuId=' + menuId), { method: 'DELETE' })
            .then(r => r.json())
            .then(res => {
                if (res.message) {
                    alert('Menu item deleted!');
                    loadRestaurantMenu();
                } else {
                    alert(res.error || 'Failed to delete');
                }
            })
            .catch(err => alert('Error: ' + err));
    }
}

// NEW FUNCTIONS FOR COUPONS, PAYMENT, AND ORDER TRACKING

let currentDiscount = 0;
let appliedCoupon = null;

function toggleAvailableCoupons() {
    const couponDiv = document.getElementById('available-coupons');
    if (couponDiv.style.display === 'none') {
        couponDiv.style.display = 'block';
        loadAvailableCoupons();
    } else {
        couponDiv.style.display = 'none';
    }
}

function loadAvailableCoupons() {
    if (!currentRestaurant) {
        alert('Please select a restaurant first');
        return;
    }
    
    const restId = currentRestaurant.id;
    console.log('Loading coupons for restaurant:', restId);
    fetch(apiUrl('/api/coupons?restaurant_id=' + restId))
        .then(r => {
            console.log('Coupon response status:', r.status);
            if (!r.ok) {
                throw new Error(`HTTP ${r.status}: ${r.statusText}`);
            }
            return r.json();
        })
        .then(coupons => {
            console.log('Coupons loaded:', coupons);
            const container = document.getElementById('coupons-list');
            if (!coupons || coupons.length === 0) {
                container.innerHTML = '<p style="color: #666;">No coupons available for this restaurant</p>';
                return;
            }

            let html = '';
            coupons.forEach(coupon => {
                let discountText = coupon.discountType === 'percentage' 
                    ? coupon.discountValue + '% off' 
                    : 'Tk ' + coupon.discountValue + ' off';
                
                html += `<div style="background: white; padding: 10px; border-radius: 4px; border: 2px solid #FF4757; cursor: pointer; text-align: center;" onclick="applyCouponFromList('${coupon.code}')">`;
                html += `<div style="font-weight: bold; color: #FF4757; font-size: 14px;">${coupon.code}</div>`;
                html += `<div style="font-size: 12px; color: #666;">${discountText}</div>`;
                if (coupon.minOrder > 0) {
                    html += `<div style="font-size: 11px; color: #999;">Min: Tk ${coupon.minOrder}</div>`;
                }
                html += `</div>`;
            });
            container.innerHTML = html;
        })
        .catch(err => {
            console.error('Error loading coupons:', err);
            document.getElementById('coupons-list').innerHTML = `<p style="color: red;">Error loading coupons: ${err.message}</p>`;
        });
}

function applyCouponFromList(code) {
    document.getElementById('coupon-input').value = code;
    applyCoupon();
}

function applyCoupon() {
    const couponCode = document.getElementById('coupon-input').value.trim();
    if (!couponCode) {
        alert('Please enter a coupon code');
        return;
    }

    if (!currentUser) {
        alert('Please login first');
        return;
    }

    if (!currentRestaurant) {
        alert('Please select a restaurant first');
        return;
    }

    const subtotal = cart.reduce((sum, item) => sum + (item.price * item.quantity), 0);
    const currentMonth = new Date().toISOString().slice(0, 7); // YYYY-MM format
    console.log('Validating coupon:', couponCode, 'for restaurant:', currentRestaurant.id, 'user:', currentUser.id, 'month:', currentMonth);

    const data = new URLSearchParams();
    data.append('code', couponCode);
    data.append('total', subtotal);
    data.append('restaurantId', currentRestaurant.id);
    data.append('userId', currentUser.id);
    data.append('monthYear', currentMonth);

    fetch(apiUrl('/api/validate-coupon'), {
        method: 'POST',
        headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
        body: data
    })
    .then(r => {
        console.log('Coupon validation response status:', r.status);
        if (!r.ok) {
            throw new Error(`HTTP ${r.status}: ${r.statusText}`);
        }
        return r.json();
    })
    .then(res => {
        console.log('Coupon response:', res);
        const messageDiv = document.getElementById('coupon-message');
        if (res.valid) {
            currentDiscount = res.discount;
            appliedCoupon = res.code;
            messageDiv.innerHTML = `<span style="color: green;">✓ Coupon applied! Discount: Tk ${res.discount.toFixed(2)}</span>`;
            document.getElementById('coupon-input').disabled = true;
            updateCartDisplay();
        } else {
            messageDiv.innerHTML = `<span style="color: red;">✗ ${res.message}</span>`;
            currentDiscount = 0;
            appliedCoupon = null;
        }
    })
    .catch(err => {
        console.error('Coupon validation error:', err);
        document.getElementById('coupon-message').innerHTML = `<span style="color: red;">Error: ${err.message}</span>`;
    });
}

function searchMenuItems() {
    const searchTerm = document.getElementById('menu-search-input').value.trim();
    if (!searchTerm) {
        alert('Please enter a search term');
        return;
    }

    fetch(apiUrl('/api/search-menu?q=' + encodeURIComponent(searchTerm)))
        .then(r => r.json())
        .then(data => {
            const resultsDiv = document.getElementById('menu-search-results');
            if (data.length === 0) {
                resultsDiv.innerHTML = '<p style="text-align: center; color: #999;">No dishes found</p>';
                return;
            }

            let html = '<div style="display: grid; grid-template-columns: repeat(auto-fill, minmax(250px, 1fr)); gap: 15px;">';
            data.forEach(item => {
                html += `<div style="background: white; padding: 15px; border-radius: 8px; box-shadow: 0 2px 8px rgba(0,0,0,0.1);">`;
                html += `<h4>${item.name}</h4>`;
                html += `<p style="color: #666; font-size: 14px;">Restaurant ID: ${item.restaurantId}</p>`;
                html += `<p style="font-size: 18px; color: #FF4757; margin: 10px 0;"><strong>Tk ${item.price.toFixed(2)}</strong></p>`;
                html += `<button onclick="addToCart(${item.id}, '${item.name}', ${item.price})" class="btn-primary" style="width: 100%; padding: 8px;">Add to Cart</button>`;
                html += `</div>`;
            });
            html += '</div>';
            resultsDiv.innerHTML = html;
        })
        .catch(err => {
            document.getElementById('menu-search-results').innerHTML = '<p style="color: red;">Error searching menu</p>';
        });
}

let orderRefreshInterval; // Global interval for auto-refresh
let restaurantCache = {}; // Cache restaurant data

function loadUserOrders() {
    if (!currentUser) {
        alert('Please login first');
        return;
    }

    goToPage('order-tracking');
    
    // Load restaurants first, then orders
    fetch(apiUrl('/api/restaurants'))
        .then(r => r.json())
        .then(restaurants => {
            // Build restaurant map
            restaurantCache = {};
            restaurants.forEach(rest => {
                restaurantCache[rest.id] = rest.name;
            });
            console.log('Restaurant cache loaded:', restaurantCache);
            
            // Now load orders
            refreshUserOrders();
            
            // Auto-refresh every 5 seconds
            if (orderRefreshInterval) {
                clearInterval(orderRefreshInterval);
            }
            orderRefreshInterval = setInterval(refreshUserOrders, 5000);
        })
        .catch(err => {
            console.error('Error loading restaurants:', err);
            // Still proceed with orders even if restaurants fail
            refreshUserOrders();
            if (orderRefreshInterval) {
                clearInterval(orderRefreshInterval);
            }
            orderRefreshInterval = setInterval(refreshUserOrders, 5000);
        });
}

function refreshUserOrders() {
    console.log('Refreshing orders for user:', currentUser.id);
    fetch(apiUrl('/api/user-orders?user_id=' + currentUser.id))
        .then(r => {
            if (!r.ok) {
                throw new Error(`HTTP ${r.status}: ${r.statusText}`);
            }
            return r.json();
        })
        .then(orders => {
            console.log('Orders loaded:', orders);
            displayOrdersTable(orders, restaurantCache);
        })
        .catch(err => {
            console.error('Error loading orders:', err);
            document.getElementById('orders-list').innerHTML = `<p style="color: red;">Error: ${err.message}</p>`;
        });
}

function displayOrdersTable(orders, restaurantMap) {
    const container = document.getElementById('orders-list');
    if (!orders || orders.length === 0) {
        container.innerHTML = '<p style="text-align: center; color: #999;">No orders yet</p>';
        return;
    }
    
    let html = '<table style="width: 100%; border-collapse: collapse;">';
    html += '<tr style="background: #f0f0f0; border-bottom: 2px solid #ddd;"><th style="padding: 12px; text-align: left;">Order ID</th><th style="padding: 12px; text-align: left;">Restaurant</th><th style="padding: 12px; text-align: left;">Status</th><th style="padding: 12px; text-align: left;">Payment</th><th style="padding: 12px; text-align: right;">Amount</th><th style="padding: 12px; text-align: center;">ETA (min)</th></tr>';

    orders.forEach(order => {
        let statusColor = '#666';
        if (order.status === 'DELIVERED') statusColor = '#4CAF50';
        if (order.status === 'OUT_FOR_DELIVERY') statusColor = '#FF9800';
        if (order.status === 'PREPARING') statusColor = '#2196F3';
        if (order.status === 'PENDING') statusColor = '#FFC107';
        if (order.status === 'READY') statusColor = '#9C27B0';
        
        let restaurantName = restaurantMap[order.restaurantId] || 'Unknown';
        console.log('Order', order.orderId, 'restaurantId:', order.restaurantId, 'name:', restaurantName, 'map:', restaurantMap);

        html += `<tr style="border-bottom: 1px solid #eee;">`;
        html += `<td style="padding: 12px;">#${order.orderId}</td>`;
        html += `<td style="padding: 12px;">${restaurantName}</td>`;
        html += `<td style="padding: 12px; color: ${statusColor}; font-weight: bold;">${order.status}</td>`;
        html += `<td style="padding: 12px;">${order.paymentStatus}</td>`;
        html += `<td style="padding: 12px; text-align: right;">Tk ${order.total.toFixed(2)}</td>`;
        html += `<td style="padding: 12px; text-align: center;">${order.deliveryTime || '-'} min</td>`;
        html += `</tr>`;
    });
    html += '</table>';
    container.innerHTML = html;
}

// USER PROFILE FUNCTIONS

function loadUserProfile() {
    if (!currentUser) {
        alert('Please login first');
        return;
    }
    
    console.log('Loading profile for user:', currentUser.id);
    
    // Pre-fill the form with current user data
    document.getElementById('profile-name').value = currentUser.name || '';
    document.getElementById('profile-email').value = currentUser.email || '';
    document.getElementById('profile-address').value = currentUser.address || '';
}

function updateProfile(e) {
    e.preventDefault();
    
    if (!currentUser) {
        alert('Please login first');
        return;
    }
    
    const name = document.getElementById('profile-name').value.trim();
    const email = document.getElementById('profile-email').value.trim();
    const address = document.getElementById('profile-address').value.trim();
    
    if (!name || !email || !address) {
        alert('All fields are required');
        return;
    }
    
    console.log('Updating profile:', { userId: currentUser.id, name, email, address });
    
    const data = new URLSearchParams();
    data.append('userId', currentUser.id);
    data.append('name', name);
    data.append('email', email);
    data.append('address', address);
    
    fetch(apiUrl('/api/update-profile'), {
        method: 'POST',
        headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
        body: data
    })
    .then(r => {
        if (!r.ok) {
            throw new Error(`HTTP ${r.status}: ${r.statusText}`);
        }
        return r.json();
    })
    .then(res => {
        console.log('Profile update response:', res);
        if (res.success || res.message) {
            alert('✓ Profile updated successfully!');
            
            // Update currentUser object
            currentUser.name = name;
            currentUser.email = email;
            currentUser.address = address;
            
            // Update display
            document.getElementById('current-user-name').textContent = 'Welcome, ' + name + '!';
            document.getElementById('profile-message').innerHTML = '<span style="color: green;">✓ Profile saved successfully</span>';
            
            setTimeout(() => {
                document.getElementById('profile-message').innerHTML = '';
            }, 3000);
        } else {
            alert('Error updating profile: ' + (res.error || 'Unknown error'));
        }
    })
    .catch(err => {
        console.error('Profile update error:', err);
        alert('Error updating profile:\n' + err.message);
    });
}

