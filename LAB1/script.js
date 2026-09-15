// ---------- DATA ----------
const COURSES = [{
    level: "200 Level",
    code: "ICT 211",
    title: "Data Structures and Algorithms",
    desc: "Core structures, complexity, and algorithm design.",
    credits: "3 credits"
}, {
    level: "200 Level",
    code: "ICT 221",
    title: "Database Systems",
    desc: "Relational design, SQL, and normalization.",
    credits: "3 credits"
}, {
    level: "200 Level",
    code: "ICT 231",
    title: "Computer Networks",
    desc: "Network architecture, protocols, and the OSI model.",
    credits: "3 credits"
}, {
    level: "200 Level",
    code: "ICT 241",
    title: "Web Application Development",
    desc: "Client-server architecture and dynamic web systems.",
    credits: "3 credits"
}, {
    level: "300 Level",
    code: "ICT 311",
    title: "Operating Systems",
    desc: "Processes, memory management, and scheduling.",
    credits: "3 credits"
}, {
    level: "300 Level",
    code: "ICT 321",
    title: "Software Engineering",
    desc: "Requirements, design methods, and the software lifecycle.",
    credits: "3 credits"
}, {
    level: "300 Level",
    code: "ICT 331",
    title: "Information Security",
    desc: "Cryptography, threats, and secure system design.",
    credits: "3 credits"
}, {
    level: "300 Level",
    code: "ICT 341",
    title: "Mobile Application Development",
    desc: "Native and cross-platform mobile app design.",
    credits: "3 credits"
}, {
    level: "400 Level",
    code: "ICT 411",
    title: "Artificial Intelligence",
    desc: "Search, knowledge representation, and machine learning basics.",
    credits: "3 credits"
}, {
    level: "400 Level",
    code: "ICT 421",
    title: "Cloud Computing",
    desc: "Virtualization, distributed systems, and cloud platforms.",
    credits: "3 credits"
}, {
    level: "400 Level",
    code: "ICT 431",
    title: "Systems Analysis and Design",
    desc: "Modeling and planning of information systems.",
    credits: "3 credits"
}, {
    level: "400 Level",
    code: "ICT 441",
    title: "Project Management in ICT",
    desc: "Planning, scheduling, and delivering ICT projects.",
    credits: "3 credits"
}];
const MAX_COURSES = 6;

// ---------- STATE (in-memory only) ----------
let selectedCodes = [];
let registeredCourses = [];
let isRegistered = false;

// ---------- RENDER COURSE LIST ----------
const courseListEl = document.getElementById('courseList');

function renderCourseList() {
    const levels = [...new Set(COURSES.map(c => c.level))];
    courseListEl.innerHTML = levels.map(level => `
      <div class="level-group">
        <div class="level-label">${level}</div>
        ${COURSES.filter(c => c.level === level).map(c => renderRow(c)).join('')}
      </div>
    `).join('');
}

function renderRow(c) {
    const checked = selectedCodes.includes(c.code);
    const atLimit = selectedCodes.length >= MAX_COURSES && !checked;
    return `
      <label class="course-row ${atLimit ? 'disabled' : ''}" for="chk-${c.code.replace(/\s/g,'')}">
        <input type="checkbox" id="chk-${c.code.replace(/\s/g,'')}"
               ${checked ? 'checked' : ''} ${atLimit ? 'disabled' : ''}
               onchange="toggleCourse('${c.code}')">
        <span class="course-code">${c.code}</span>
        <span class="course-title">${c.title}<span class="course-desc">${c.desc}</span></span>
        <span class="course-credits">${c.credits}</span>
      </label>
    `;
}

function toggleCourse(code) {
    const idx = selectedCodes.indexOf(code);
    if (idx > -1) {
        selectedCodes.splice(idx, 1);
    } else {
        if (selectedCodes.length >= MAX_COURSES) return;
        selectedCodes.push(code);
    }
    updateSidebar();
    renderCourseList();
}

// ---------- SIDEBAR ----------
const selCountEl = document.getElementById('selCount');
const progressFillEl = document.getElementById('progressFill');
const selectedListEl = document.getElementById('selectedList');
const limitNoteEl = document.getElementById('limitNote');
const confirmBtnEl = document.getElementById('confirmBtn');

function updateSidebar() {
    selCountEl.textContent = selectedCodes.length;
    progressFillEl.style.width = (selectedCodes.length / MAX_COURSES * 100) + '%';

    if (selectedCodes.length === 0) {
        selectedListEl.innerHTML = '<li class="empty">No courses selected yet</li>';
    } else {
        selectedListEl.innerHTML = selectedCodes.map(code => {
            const c = COURSES.find(x => x.code === code);
            return `<li><span>${c.code}</span><span>${c.credits}</span></li>`;
        }).join('');
    }

    limitNoteEl.textContent = selectedCodes.length >= MAX_COURSES ?
        'Maximum of 6 courses reached for this semester.' :
        '';

    confirmBtnEl.disabled = selectedCodes.length === 0;
}

// ---------- OVERLAYS: open / close ----------
function openOverlay(id) {
    document.getElementById(id).classList.add('show');
    setActiveTab(id);
    if (id === 'coursesOverlay') renderRegisteredOverlay();
    if (id === 'studentOverlay') renderStudentOverlay();
}

function closeOverlay(id) {
    document.getElementById(id).classList.remove('show');
    setActiveTab(null);
}
document.querySelectorAll('.overlay').forEach(ov => {
    ov.addEventListener('click', (e) => {
        if (e.target === ov) closeOverlay(ov.id);
    });
});

function setActiveTab(overlayId) {
    document.getElementById('navRegister').classList.toggle('active', overlayId === null);
    document.getElementById('navCourses').classList.toggle('active', overlayId === 'coursesOverlay');
    document.getElementById('navStudent').classList.toggle('active', overlayId === 'studentOverlay');
}

function showRegistration() {
    document.querySelectorAll('.overlay.show').forEach(ov => ov.classList.remove('show'));
    setActiveTab(null);
    window.scrollTo({
        top: 0,
        behavior: 'smooth'
    });
}

// ---------- CONFIRM-SELECTION OVERLAY ----------
function openConfirmOverlay() {
    const list = document.getElementById('confirmList');
    list.innerHTML = selectedCodes.map(code => {
        const c = COURSES.find(x => x.code === code);
        return `<li><span class="code">${c.code}</span><span class="name">${c.title}</span></li>`;
    }).join('');
    openOverlay('confirmOverlay');
    setActiveTab(null); // registration is still the underlying page
}

function finalizeRegistration() {
    registeredCourses = [...selectedCodes];
    isRegistered = registeredCourses.length > 0;
    closeOverlay('confirmOverlay');
    showBanner(`Successfully registered for <strong>${registeredCourses.length}</strong> course${registeredCourses.length === 1 ? '' : 's'} this semester.`);
}

// ---------- BANNER ----------
const bannerEl = document.getElementById('banner');
const bannerTextEl = document.getElementById('bannerText');
let bannerTimer;

function showBanner(html) {
    bannerTextEl.innerHTML = html;
    bannerEl.classList.add('show');
    clearTimeout(bannerTimer);
    bannerTimer = setTimeout(hideBanner, 6000);
}

function hideBanner() {
    bannerEl.classList.remove('show');
}

// ---------- REGISTERED COURSES OVERLAY ----------
function renderRegisteredOverlay() {
    const body = document.getElementById('registeredCoursesBody');
    if (registeredCourses.length === 0) {
        body.innerHTML = `
        <p>You haven't confirmed any courses yet.</p>
        <div class="empty-state">No registered courses. Go to Registration to make your selection.</div>
      `;
        return;
    }
    body.innerHTML = `
      <p>You are registered for ${registeredCourses.length} course${registeredCourses.length === 1 ? '' : 's'} this semester.</p>
      <ul class="reg-list">
        ${registeredCourses.map(code => {
          const c = COURSES.find(x => x.code === code);
          return `<li><span class="code">${c.code}</span><span class="name">${c.title}</span></li>`;
        }).join('')}
      </ul>
    `;
  }

  // ---------- STUDENT ID OVERLAY ----------
  function renderStudentOverlay(){
    const dot = document.getElementById('statusDot');
    const text = document.getElementById('statusText');
    const note = document.getElementById('statusNote');
    if (isRegistered){
      dot.className = 'status-dot reg';
      text.textContent = `Registered for ${registeredCourses.length} course${registeredCourses.length === 1 ? '' : 's'} this semester`;
      note.textContent = 'Registration is complete. View the full list under My Courses.';
    } else {
      dot.className = 'status-dot unreg';
      text.textContent = 'Not yet registered for this semester';
      note.textContent = 'Select your courses from the Registration tab, then confirm to complete registration.';
    }
  }

  // ---------- INIT ----------
  renderCourseList();
  updateSidebar();