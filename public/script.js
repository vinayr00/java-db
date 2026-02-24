document.addEventListener('DOMContentLoaded', () => {
    // === NAVIGATION LOGIC ===
    // Select all navigation buttons and content views
    const navButtons = document.querySelectorAll('.nav-btn');
    const views = document.querySelectorAll('.view');
    const pageTitle = document.getElementById('page-title');

    navButtons.forEach(btn => {
        btn.addEventListener('click', () => {
            const target = btn.getAttribute('data-target');

            // 1. Update Active State of Buttons (highlight the clicked one)
            navButtons.forEach(b => b.classList.remove('active'));
            btn.classList.add('active');

            // 2. Switch Visibility of Views (show only the target section)
            views.forEach(v => v.classList.remove('active'));
            document.getElementById(target).classList.add('active');

            // 3. Update the Page Header Title
            pageTitle.textContent = btn.textContent;

            // 4. Trigger Data Refresh when moving to Dashboard or Student List
            if (target === 'view-students' || target === 'dashboard') {
                fetchStudents();
            }
        });
    });

    // === API CALL: ADD STUDENT (POST) ===
    const studentForm = document.getElementById('student-form');
    studentForm.addEventListener('submit', async (e) => {
        e.preventDefault(); // Stop the page from reloading on submit

        // Collect all data from the form fields
        const student = {
            register_number: document.getElementById('regNum').value,
            name: document.getElementById('name').value,
            department: document.getElementById('dept').value,
            year: document.getElementById('year').value,
            phone: document.getElementById('phone').value,
            email: document.getElementById('email').value
        };

        try {
            // API CALL: Send a POST request to '/api/students' with the student data as JSON
            const response = await fetch('/api/students', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(student)
            });

            if (response.ok) {
                alert('Student added successfully!');
                studentForm.reset(); // Clear the form
                fetchStudents();     // Refresh the list in the background
            } else {
                const error = await response.text();
                alert('Error: ' + error);
            }
        } catch (err) {
            alert('Failed to connect to server');
        }
    });

    // === API CALL: FETCH ALL STUDENTS (GET) ===
    async function fetchStudents() {
        try {
            // API CALL: Send a GET request to retrieve the full list of students
            const response = await fetch('/api/students');
            const students = await response.json();

            // Send the data to helper functions to update the UI
            updateTable(students);
            updateDashboard(students);
        } catch (err) {
            console.error('Failed to fetch students:', err);
        }
    }

    // Helper: Build the HTML Table from the student data list
    function updateTable(students) {
        const tableBody = document.getElementById('student-table-body');
        tableBody.innerHTML = ''; // Clear old rows

        students.forEach(s => {
            const row = document.createElement('tr');
            row.innerHTML = `
                <td>${s.register_number}</td>
                <td>${s.name}</td>
                <td>${s.department}</td>
                <td>${s.year}</td>
                <td>
                    <!-- Link the delete button to the global delete function -->
                    <button class="delete-btn" onclick="deleteStudent('${s.register_number}')">Delete</button>
                </td>
            `;
            tableBody.appendChild(row);
        });
    }

    // Helper: Update the number on the dashboard stat cards
    function updateDashboard(students) {
        document.getElementById('total-count').textContent = students.length;
    }

    // Refresh Button Listener
    document.getElementById('refresh-btn').addEventListener('click', fetchStudents);

    // Initial Load: Populates data as soon as the website opens
    fetchStudents();
});

// === API CALL: DELETE STUDENT (DELETE) ===
// This function is global so it can be called from the HTML buttons in the table
async function deleteStudent(regNum) {
    if (!confirm(`Are you sure you want to delete student ${regNum}?`)) return;

    try {
        // API CALL: Send a DELETE request with the student ID as a URL parameter (?id=...)
        const response = await fetch(`/api/students?id=${regNum}`, {
            method: 'DELETE'
        });

        if (response.ok) {
            alert('Student deleted successfully');
            location.reload(); // Refresh the page to show the updated list
        } else {
            alert('Failed to delete student');
        }
    } catch (err) {
        alert('Error connecting to server');
    }
}
