const loginPanel = document.getElementById('loginPanel');
const votingPanel = document.getElementById('votingPanel');
const participantNameInput = document.getElementById('participantName');
const enterButton = document.getElementById('enterButton');
const participantLabel = document.getElementById('participantLabel');
const questionText = document.getElementById('questionText');
const progressBar = document.getElementById('progressBar');
const timerText = document.getElementById('timerText');
const optionsElement = document.getElementById('options');
const resultsElement = document.getElementById('results');
const messageElement = document.getElementById('message');

let participantName = localStorage.getItem('voterraParticipantName') || '';
let selectedOptionIds = new Set();
let activeSessionId = null;

if (participantName) {
  enterAudience();
}

enterButton.addEventListener('click', () => {
  participantName = participantNameInput.value.trim();
  if (!participantName) {
    participantNameInput.focus();
    return;
  }
  localStorage.setItem('voterraParticipantName', participantName);
  enterAudience();
});

function enterAudience() {
  loginPanel.classList.add('hidden');
  votingPanel.classList.remove('hidden');
  participantLabel.textContent = participantName;
  pollState();
  setInterval(pollState, 1000);
}

async function pollState() {
  const response = await fetch('/api/state');
  const state = await response.json();
  renderState(state);
}

function renderState(state) {
  if (!state.active) {
    activeSessionId = null;
    selectedOptionIds.clear();
    questionText.textContent = 'Waiting for an active question';
    optionsElement.innerHTML = '';
    resultsElement.innerHTML = '';
    progressBar.style.width = '0%';
    timerText.textContent = '';
    return;
  }

  if (activeSessionId !== state.session.sessionId) {
    activeSessionId = state.session.sessionId;
    selectedOptionIds.clear();
    messageElement.textContent = '';
  }

  questionText.textContent = state.session.question.text;
  const durationMillis = state.session.question.durationSeconds * 1000;
  const remainingPercent = Math.max(0, Math.min(100, (state.remainingMillis / durationMillis) * 100));
  progressBar.style.width = `${remainingPercent}%`;
  timerText.textContent = state.open
    ? `${Math.ceil(state.remainingMillis / 1000)} seconds remaining`
    : 'Voting is closed';

  if (state.open) {
    renderOptions(state.session.question);
    resultsElement.innerHTML = '';
  } else {
    optionsElement.innerHTML = '';
    renderResults(state.session.question, state.summary);
  }
}

function renderOptions(question) {
  optionsElement.innerHTML = '';
  for (const option of question.options) {
    const button = document.createElement('button');
    button.className = 'option-button';
    if (selectedOptionIds.has(option.id)) {
      button.classList.add('selected');
    }
    button.textContent = option.text;
    button.addEventListener('click', () => selectOption(question, option.id));
    optionsElement.append(button);
  }
}

async function selectOption(question, optionId) {
  if (question.multipleChoice) {
    if (selectedOptionIds.has(optionId)) {
      selectedOptionIds.delete(optionId);
    } else {
      selectedOptionIds.add(optionId);
    }
  } else {
    selectedOptionIds = new Set([optionId]);
  }

  await fetch('/api/vote', {
    method: 'POST',
    headers: {'Content-Type': 'application/json'},
    body: JSON.stringify({
      participantName,
      optionIds: [...selectedOptionIds]
    })
  });
  messageElement.textContent = 'Vote saved';
  renderOptions(question);
}

function renderResults(question, summary) {
  resultsElement.innerHTML = '';
  for (const option of question.options) {
    const row = document.createElement('div');
    row.className = 'result-row';
    const label = document.createElement('span');
    label.textContent = option.text;
    const count = document.createElement('strong');
    count.textContent = summary.countsByOptionId[option.id] || 0;
    row.append(label, count);
    resultsElement.append(row);
  }
}
