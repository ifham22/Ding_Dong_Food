let cart = [];

document.addEventListener('DOMContentLoaded', () => {
    fetchRestaurants();
    fetchUsers();
    document.getElementById('register-form').addEventListener('submit', registerRestaurant);
    document.getElementById('add-menu-form').addEventListener('submit', addMenuItem);
    document.getElementById('register-user-form').addEventListener('submit', registerUser);
    document.getElementById('schedule-form').addEventListener('submit', scheduleOrder);
});

function fetchRestaurants(url = '/api/restaurants') {
    const container = document.getElementById('restaurant-container');
    const status = document.getElementById('status');
    
    status.innerText = 'Refreshing...';
    
    // Fetch directly from our Java Server API
    fetch(url)
        .then(response => {
            if (!response.ok) {
                throw new Error('Network response was not ok');
            }
            return response.json();
        })
        .then(data => {
            container.innerHTML = ''; // clear
            
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

function populateSelect(restaurants) {
    const select = document.getElementById('restaurant-select');
    select.innerHTML = '<option value="">Select Restaurant</option>';
    restaurants.forEach(r => {
        const option = document.createElement('option');
        option.value = r.id;
        option.textContent = r.name;
        select.appendChild(option);
    });
}

function searchRestaurants() {
    const q = document.getElementById('search-input').value;
    fetchRestaurants('/api/search?q=' + encodeURIComponent(q));
}

function registerRestaurant(e) {
    e.preventDefault();
    const formData = new FormData(e.target);
    const data = new URLSearchParams(formData);
    fetch('/api/register-restaurant', {
        method: 'POST',
        headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
        body: data
    })
    .then(r => r.json())
    .then(res => {
        alert(res.message || res.error);
        if (res.message) {
            fetchRestaurants(); // refresh list
        }
    })
    .catch(err => alert('Error: ' + err));
}

function addMenuItem(e) {
    e.preventDefault();
    const formData = new FormData(e.target);
    const data = new URLSearchParams(formData);
    fetch('/api/add-menu', {
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

function viewMenu(restId, restName) {
    fetch('/api/menu?restId=' + restId)
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
                    
                    const descCell = row.insertCell();
                    descCell.style.display = 'none';
                    
                    const priceCell = row.insertCell();
                    priceCell.textContent = `Tk${item.price}`;
                    priceCell.style.padding = '10px';
                    priceCell.style.textAlign = 'right';
                    
                    const qtyCell = row.insertCell();
                    qtyCell.textContent = item.quantity;
                    qtyCell.style.padding = '10px';
                    qtyCell.style.textAlign = 'right';
                    
                    const actionCell = row.insertCell();
                    actionCell.innerHTML = `
                        <button onclick="addToCart(${restId}, ${item.id}, ${item.price}, '${item.name}')" style="background: #3c90ff; color: white; border: none; padding: 5px 10px; border-radius: 3px; cursor: pointer; margin-right: 5px;">Add</button>
                        <button onclick="deleteMenuItem(${item.id})" style="background: #ff6b6b; color: white; border: none; padding: 5px 10px; border-radius: 3px; cursor: pointer;">Delete</button>`;
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
    updateCartSummary();
    renderCart();
}

function updateCartSummary() {
    const summary = document.getElementById('cart-summary');
    const totalItems = cart.reduce((sum, i) => sum + i.quantity, 0);
    const totalCost = cart.reduce((sum, i) => sum + i.quantity * i.price, 0).toFixed(2);
    summary.textContent = `${totalItems} item(s), Tk ${totalCost}`;
    renderCart();
}

function renderCart() {
    const cartDiv = document.getElementById('cart-items');
    if (cart.length === 0) {
        cartDiv.innerHTML = 'Your cart is empty.';
        return;
    }

    let html = '<table style="width:100%; border-collapse: collapse; margin-top: 10px;">';
    html += '<tr style="background: #f0f0f0;"><th>Item</th><th>Qty</th><th>Price</th><th>Total</th><th>Actions</th></tr>';

    cart.forEach(item => {
        html += `<tr>`;
        html += `<td>${item.name}</td>`;
        html += `<td>${item.quantity}</td>`;
        html += `<td>Tk ${item.price.toFixed(2)}</td>`;
        html += `<td>Tk ${(item.quantity * item.price).toFixed(2)}</td>`;
        html += `<td>`;
        html += `<button onclick="changeQuantity(${item.menuId}, -1)" style="margin-right: 5px;">-</button>`;
        html += `<button onclick="changeQuantity(${item.menuId}, 1)">+</button>`;
        html += `<button onclick="removeFromCart(${item.menuId})" style="margin-left: 5px; background: #ff6b6b; color: white;">Remove</button>`;
        html += `</td>`;
        html += `</tr>`;
    });
    html += '</table>';

    cartDiv.innerHTML = html;
}

function removeFromCart(menuId) {
    cart = cart.filter(i => i.menuId !== menuId);
    if (cart.length === 0) {
        // allow new restaurant
    }
    updateCartSummary();
    renderCart();
}

function changeQuantity(menuId, delta) {
    const item = cart.find(i => i.menuId === menuId);
    if (!item) return;
    item.quantity += delta;
    if (item.quantity <= 0) {
        cart = cart.filter(i => i.menuId !== menuId);
    }
    updateCartSummary();
    renderCart();
}

function checkoutOrder() {
    if (cart.length === 0) {
        alert('Cart is empty. Add items first!');
        return;
    }
    const userId = prompt('Enter user ID to place order for (use the registered user list):');
    if (!userId || isNaN(userId)) {
        alert('Invalid user ID.');
        return;
    }
    const restId = cart[0].restId;
    const items = cart.map(i => `${i.menuId}:${i.quantity}`).join(';');

    fetch('/api/place-order', {
        method: 'POST',
        headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
        body: `userId=${encodeURIComponent(userId)}&restId=${encodeURIComponent(restId)}&items=${encodeURIComponent(items)}`
    })
    .then(r => r.json())
    .then(res => {
        alert(res.message || res.error);
        if (res.message) {
            cart = [];
            updateCartSummary();
            renderCart();
            fetchRestaurants();
        }
    })
    .catch(err => alert('Error: ' + err));
}

function fetchUsers() {
    fetch('/api/users')
        .then(r => r.json())
        .then(data => {
            const list = document.getElementById('user-list');
            if (data.error) {
                list.innerHTML = '<p style="color:red;">' + data.error + '</p>';
                return;
            }
            if (!Array.isArray(data) || data.length === 0) {
                list.innerHTML = '<p>No users registered yet.</p>';
                return;
            }
            let html = '<table style="width:100%; border-collapse: collapse;"><tr><th>ID</th><th>Name</th><th>Email</th><th>Address</th></tr>';
            data.forEach(u => {
                html += `<tr><td>${u.id}</td><td>${u.name}</td><td>${u.email}</td><td>${u.address}</td></tr>`;
            });
            html += '</table>';
            list.innerHTML = html;
        })
        .catch(err => {
            document.getElementById('user-list').innerHTML = '<p style="color:red;">Failed to fetch users: ' + err + '</p>';
        });
}

function registerUser(e) {
    e.preventDefault();
    const formData = new FormData(e.target);
    const data = new URLSearchParams(formData);

    fetch('/api/register-user', {
        method: 'POST',
        headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
        body: data
    })
    .then(r => r.json())
    .then(res => {
        alert(res.message || res.error);
        if (res.message) {
            e.target.reset();
            fetchUsers();
        }
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

    fetch('/api/schedule-order', {
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

function deleteMenuItem(menuId) {
    if (confirm('Are you sure you want to delete this item?')) {
        fetch('/api/delete-menu?menuId=' + menuId, { method: 'DELETE' })
            .then(r => r.json())
            .then(res => {
                alert(res.message || res.error);
                const restIdSelect = document.getElementById('restaurant-select');
                if (restIdSelect.value) {
                    viewMenu(restIdSelect.value, document.getElementById('menu-restaurant-name').textContent.replace(' - Menu', ''));
                }
            })
            .catch(err => alert('Error: ' + err));
    }
}

// Duplicate deleteMenuItem removed. Single implementation above is enough.

