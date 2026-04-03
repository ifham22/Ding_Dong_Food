document.addEventListener('DOMContentLoaded', () => {
    fetchRestaurants();
    document.getElementById('register-form').addEventListener('submit', registerRestaurant);
    document.getElementById('add-menu-form').addEventListener('submit', addMenuItem);
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
                    actionCell.innerHTML = `<button onclick="deleteMenuItem(${item.id})" style="background: #ff6b6b; color: white; border: none; padding: 5px 10px; border-radius: 3px; cursor: pointer;">Delete</button>`;
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
