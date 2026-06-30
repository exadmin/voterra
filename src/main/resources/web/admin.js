const adminLoginPanel = document.getElementById('adminLoginPanel');
const adminPanel = document.getElementById('adminPanel');
const adminPassword = document.getElementById('adminPassword');
const adminEnterButton = document.getElementById('adminEnterButton');
const adminLoginMessage = document.getElementById('adminLoginMessage');
const questionList = document.getElementById('questionList');
const adminProgressPanel = document.getElementById('adminProgressPanel');
const adminQuestionText = document.getElementById('adminQuestionText');
const adminProgressBar = document.getElementById('adminProgressBar');
const adminTimerText = document.getElementById('adminTimerText');
const adminState = document.getElementById('adminState');

let adminToken = sessionStorage.getItem('voterraAdminToken') || '';
let activateButtons = [];
let statePollerId = null;

if (adminToken) {
  enterAdmin();
}

adminEnterButton.addEventListener('click', async () => {
  const response = await fetch('/api/admin/login', {
    method: 'POST',
    headers: {'Content-Type': 'application/json'},
    body: JSON.stringify({password: adminPassword.value})
  });
  const body = await response.json();
  if (!response.ok) {
    adminLoginMessage.textContent = body.error || 'Cannot sign in';
    adminLoginMessage.classList.add('error');
    return;
  }
  adminToken = body.token;
  sessionStorage.setItem('voterraAdminToken', adminToken);
  adminLoginMessage.textContent = '';
  adminLoginMessage.classList.remove('error');
  enterAdmin();
});

async function enterAdmin() {
  adminLoginPanel.classList.add('hidden');
  adminPanel.classList.remove('hidden');
  const loaded = await loadQuestions();
  if (!loaded) {
    return;
  }
  await pollAdminState();
  if (!statePollerId) {
    statePollerId = setInterval(pollAdminState, 1000);
  }
}

async function loadQuestions() {
  const response = await fetch('/api/admin/questions', {
    headers: {'X-Admin-Token': adminToken}
  });
  const body = await response.json();
  if (!response.ok) {
    resetAdminSession(body.error || 'Admin session expired. Sign in again.');
    return false;
  }

  questionList.innerHTML = '';
  activateButtons = [];
  for (const question of body.questions || []) {
    const item = document.createElement('div');
    item.className = 'question-item';
    const text = document.createElement('span');
    text.textContent = `${question.text} (${question.durationSeconds}s)`;
    const button = document.createElement('button');
    button.className = 'secondary';
    button.textContent = 'Activate';
    button.addEventListener('click', () => activateQuestion(question.id));
    activateButtons.push(button);
    item.append(text, button);
    questionList.append(item);
  }
  return true;
}

async function activateQuestion(questionId) {
  setActivationLocked(true);
  const response = await fetch('/api/admin/activate', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'X-Admin-Token': adminToken
    },
    body: JSON.stringify({questionId})
  });
  if (!response.ok) {
    const body = await response.json();
    adminTimerText.textContent = body.error || 'Cannot activate question';
    adminTimerText.classList.add('error');
  }
  await pollAdminState();
}

async function pollAdminState() {
  const response = await fetch('/api/state');
  const state = await response.json();
  adminState.innerHTML = '';
  setActivationLocked(state.active && state.open);
  if (!state.active) {
    adminProgressPanel.classList.add('hidden');
    adminState.textContent = 'No active question';
    return;
  }

  renderAdminProgress(state);

  const title = document.createElement('div');
  title.className = 'result-row';
  title.innerHTML = `<span>${state.session.question.text}</span><strong>${state.open ? 'Open' : 'Closed'}</strong>`;
  adminState.append(title);

  const totalVotes = Object.values(state.summary.countsByOptionId)
    .reduce((sum, optionCount) => sum + optionCount, 0);
  for (const option of state.session.question.options) {
    const row = document.createElement('div');
    row.className = 'result-row';
    const count = state.summary.countsByOptionId[option.id] || 0;
    const details = document.createElement('div');
    details.className = 'result-details';
    const label = document.createElement('span');
    label.textContent = option.text;
    const track = document.createElement('div');
    track.className = 'result-progress-track';
    const bar = document.createElement('div');
    bar.className = 'result-progress-bar';
    bar.style.width = totalVotes === 0 ? '0%' : `${(count / totalVotes) * 100}%`;
    track.append(bar);
    details.append(label, track);
    const countElement = document.createElement('strong');
    countElement.textContent = count;
    row.append(details, countElement);
    adminState.append(row);
  }
}

function renderAdminProgress(state) {
  adminProgressPanel.classList.remove('hidden');
  adminQuestionText.textContent = state.session.question.text;
  const durationMillis = state.session.question.durationSeconds * 1000;
  const remainingPercent = Math.max(0, Math.min(100, (state.remainingMillis / durationMillis) * 100));
  adminProgressBar.style.width = `${remainingPercent}%`;
  adminTimerText.classList.remove('error');
  adminTimerText.textContent = state.open
    ? `${Math.ceil(state.remainingMillis / 1000)} seconds remaining`
    : 'Voting is closed. You can activate another question.';
}

function setActivationLocked(locked) {
  for (const button of activateButtons) {
    button.disabled = locked;
  }
}

function resetAdminSession(message) {
  adminToken = '';
  sessionStorage.removeItem('voterraAdminToken');
  if (statePollerId) {
    clearInterval(statePollerId);
    statePollerId = null;
  }
  adminPanel.classList.add('hidden');
  adminLoginPanel.classList.remove('hidden');
  questionList.innerHTML = '';
  adminState.innerHTML = '';
  adminLoginMessage.textContent = message;
  adminLoginMessage.classList.add('error');
}
