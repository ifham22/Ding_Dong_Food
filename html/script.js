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

function searchRestaurants() {
    const q = document.getElementById('search-input').value;
    fetchRestaurants(apiUrl('/api/search?q=' + encodeURIComponent(q)));
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
                table.innerHTML = '<tr style="background: #f0f0f0; border-bottom: 1px solid #ccc;"><th style="padding: 10px; text-align: left;">Item & Description</th><th style="padding: 10px; text-align: right;">Price</th><th style="padding: 10px; text-align: right;">Qty</th><th style="padding: 10px; text-align: center;">Action</th></tr>';
                
                data.forEach(item => {
                    const row = table.insertRow();
                    row.style.borderBottom = '1px solid #eee';
                    const itemCell = row.insertCell();
                    itemCell.innerHTML = `<strong>${item.name}</strong><br><small style="color: #666;">${item.description}</small>`;
                    itemCell.style.padding = '10px';
                    
                    const priceCell = row.insertCell();
                    priceCell.textContent = `Tk${item.price}`;
                    priceCell.style.padding = '10px';
                    priceCell.style.textAlign = 'right';
                    
                    const qtyCell = row.insertCell();
                    qtyCell.textContent = item.quantity;
                    qtyCell.style.padding = '10px';
                    qtyCell.style.textAlign = 'right';
                    
                    const actionCell = row.insertCell();
                    actionCell.innerHTML = `<button onclick="addToCart(${restId}, ${item.id}, ${item.price}, '${item.name}')" style="background: #3c90ff; color: white; border: none; padding: 5px 10px; border-radius: 3px; cursor: pointer;">Add</button>`;
                    actionCell.style.padding = '10px';
                    actionCell.style.textAlign = 'center';
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

function addToCart(restId, menuId, price, name) {
    if (cart.length > 0 && cart[0].restId !== restId) {
        alert('You can only order from one restaurant at a time. Please clear the cart first.');
        return;
    }

    const item = cart.find(i => i.menuId === menuId);
    if (!item) {
        cart.push({ restId, menuId, quantity: 1, price, name });
    } else {
        item.quantity += 1;
    }
    alert('Added to cart!');
    updateCartDisplay();
}

function updateCartDisplay() {
    const totalItems = cart.reduce((sum, i) => sum + i.quantity, 0);
    const totalCost = cart.reduce((sum, i) => sum + i.quantity * i.price, 0).toFixed(2);
    
    const cartItemsDiv = document.getElementById('cart-items');
    const cartTotalDiv = document.getElementById('cart-total');
    
    if (!cartItemsDiv) return;
    
    if (cart.length === 0) {
        cartItemsDiv.innerHTML = '<p>Your cart is empty.</p>';
        if (cartTotalDiv) cartTotalDiv.textContent = 'Tk 0.00';
        return;
    }

    let html = '<table style="width:100%; border-collapse: collapse; margin-top: 10px;">';
    html += '<tr style="background: #f0f0f0;"><th style="padding: 10px; text-align: left;">Item</th><th style="padding: 10px; text-align: center;">Qty</th><th style="padding: 10px; text-align: right;">Price</th><th style="padding: 10px; text-align: right;">Total</th><th style="padding: 10px; text-align: center;">Actions</th></tr>';

    cart.forEach(item => {
        html += `<tr style="border-bottom: 1px solid #eee;">`;
        html += `<td style="padding: 10px;">${item.name}</td>`;
        html += `<td style="padding: 10px; text-align: center;">${item.quantity}</td>`;
        html += `<td style="padding: 10px; text-align: right;">Tk ${item.price.toFixed(2)}</td>`;
        html += `<td style="padding: 10px; text-align: right;">Tk ${(item.quantity * item.price).toFixed(2)}</td>`;
        html += `<td style="padding: 10px; text-align: center;">`;
        html += `<button onclick="changeQuantity(${item.menuId}, -1)" style="margin-right: 5px; padding: 5px 10px;">-</button>`;
        html += `<button onclick="changeQuantity(${item.menuId}, 1)" style="margin-right: 5px; padding: 5px 10px;">+</button>`;
        html += `<button onclick="removeFromCart(${item.menuId})" style="background: #ff6b6b; color: white; padding: 5px 10px; border: none; border-radius: 3px; cursor: pointer;">Remove</button>`;
        html += `</td>`;
        html += `</tr>`;
    });
    html += '</table>';

    cartItemsDiv.innerHTML = html;
    if (cartTotalDiv) cartTotalDiv.textContent = 'Tk ' + totalCost;
}

function removeFromCart(menuId) {
    cart = cart.filter(i => i.menuId !== menuId);
    updateCartDisplay();
}

function changeQuantity(menuId, delta) {
    const item = cart.find(i => i.menuId === menuId);
    if (!item) return;
    item.quantity += delta;
    if (item.quantity <= 0) {
        cart = cart.filter(i => i.menuId !== menuId);
    }
    updateCartDisplay();
}

function clearCart() {
    cart = [];
    updateCartDisplay();
}

function checkoutOrder() {
    if (cart.length === 0) {
        alert('Cart is empty. Add items first!');
        return;
    }
    if (!currentUser) {
        alert('Please login first as a user to place order.');
        return;
    }
    const userId = currentUser.id;
    const restId = cart[0].restId;
    const items = cart.map(i => `${i.menuId}:${i.quantity}`).join(';');
    
    console.log('Placing order:', { userId, restId, items, cart });
    const requestBody = `userId=${encodeURIComponent(userId)}&restId=${encodeURIComponent(restId)}&items=${encodeURIComponent(items)}`;
    console.log('Request body:', requestBody);

    fetch(apiUrl('/api/place-order'), {
        method: 'POST',
        headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
        body: requestBody
    })
    .then(r => {
        console.log('Response status:', r.status);
        return r.json();
    })
    .then(res => {
        console.log('Order response:', res);
        if (res.message) {
            alert('Order placed successfully!');
            cart = [];
            updateCartDisplay();
            fetchRestaurants();
            goToPage('main-app');
        } else {
            alert('Error: ' + (res.error || 'Order failed'));
        }
    })
    .catch(err => {
        console.error('Order error:', err);
        alert('Error: ' + err);
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

