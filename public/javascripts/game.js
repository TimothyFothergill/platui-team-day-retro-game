// ============================================================
// GAME STATE
// ============================================================
let gameState = {
  playerName: 'Player',
  hp: 100,
  maxHp: 100,
  gold: 50,
  level: 1,
  currentXp: 1,
  currentArea: 'home',
  inventory: [],
  equipment: {},
  combatActive: false,
  enemyHp: 0,
  enemyMaxHp: 0,
  currentQuestion: null
};

// ============================================================
// INITIALIZATION
// ============================================================
document.addEventListener('DOMContentLoaded', () => {
  fetchPlayerState();
});

async function fetchPlayerState() {
  try {
    const res = await fetch('/player/state');
    const data = await res.json();
    if (data.playerName) {
      gameState = { ...gameState, ...data };
      updateUI();
    }
  } catch (err) {
    console.log('Player state not available yet.');
  }
}

// ============================================================
// NAVIGATION
// ============================================================
async function navigateTo(area) {
  try {
    const res = await fetch(`/player/navigate/${area}`, { method: 'POST' });
    const data = await res.json();

    if (data.success) {
      gameState.currentArea = area;

      // Update nav buttons
      document.querySelectorAll('.nav-btn').forEach(btn => btn.classList.remove('active'));
      const activeBtn = document.getElementById(`btn-${area}`);
      if (activeBtn) activeBtn.classList.add('active');

      // Show the correct view
      document.querySelectorAll('.area-view').forEach(view => view.classList.add('hidden'));
      const targetView = document.getElementById(`${area}-view`);
      if (targetView) targetView.classList.remove('hidden');

      // If navigating to public zone, show combat or no-combat UI
      if (area === 'publicZone') {
        if (gameState.combatActive) {
          document.getElementById('combat-area').classList.remove('hidden');
          document.getElementById('no-combat-area').classList.add('hidden');
        } else {
          document.getElementById('combat-area').classList.add('hidden');
          document.getElementById('no-combat-area').classList.remove('hidden');
        }
      }

      updateUI();
    }
  } catch (err) {
    console.error('Navigation failed:', err);
  }
}

// ============================================================
// COMBAT
// ============================================================
async function startBattle() {
  try {
    const res = await fetch('/combat/start', { method: 'POST' });
    const data = await res.json();

    if (data.combatActive) {
      gameState.enemyHp = data.enemy.hp;
      gameState.enemyMaxHp = data.enemy.hp;
      document.getElementById('enemy-sprite').src = `/assets/images/${data.enemy.imagePath}`;
      document.getElementById('enemy-name').textContent = data.enemy.name;
      document.getElementById('enemy-level').textContent = `lv ${data.enemy.level}`;
      updateEnemyHpBar();

      document.getElementById('no-combat-area').classList.add('hidden');
      document.getElementById('combat-area').classList.remove('hidden');
    }
  } catch (err) {
    console.error('Failed to start combat:', err);
  }
}

async function attackEnemy() {
  if (!gameState.combatActive) return;

  try {
    
    const res = await fetch('/combat/attack', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({})
    });
    const data = await res.json();

    addCombatLog(data.message);
    gameState.hp = data.playerHp;
    updateHPBar();

    if (data.killed) {
      gameState.combatActive = false;
      document.getElementById('enemy-display').style.display = 'none';

      // Show combat question!
      showCombatQuestion(() => {
        addCombatLog(`🎉 Enemy defeated! You earned ${data.reward} gold!`);
        setTimeout(() => {
          document.getElementById('combat-area').classList.add('hidden');
          document.getElementById('no-combat-area').classList.remove('hidden');
        }, 1500);
      });
    }
  } catch (err) {
    console.error('Attack failed:', err);
  }
}

async function endCombat() {
  try {
    await fetch('/combat/end', { method: 'POST' });
    gameState.combatActive = false;
    document.getElementById('combat-area').classList.add('hidden');
    document.getElementById('no-combat-area').classList.remove('hidden');
    addCombatLog('You ran away safely.');
  } catch (err) {
    console.error('Failed to end combat:', err);
  }
}

// ============================================================
// COMBAT QUESTIONS (The Retro Game Twist!)
// ============================================================
async function showCombatQuestion(onCorrect) {
  try {
    const res = await fetch('/questions');
    const data = await res.json();

    // Pick a random question
    const qIndex = Math.floor(Math.random() * data.questions.length);
    const question = data.questions[qIndex];
    gameState.currentQuestion = question;

    document.getElementById('question-text').textContent = question.question;

    const optionsContainer = document.getElementById('options-container');
    optionsContainer.innerHTML = '';

    question.options.forEach((option, idx) => {
      const btn = document.createElement('button');
      btn.className = 'option-btn';
      btn.textContent = option;
      btn.onclick = () => submitAnswer(option, onCorrect);
      optionsContainer.appendChild(btn);
    });

    document.getElementById('question-modal').classList.remove('hidden');
  } catch (err) {
    console.error('Failed to get question:', err);
    if (onCorrect) onCorrect();
  }
}

function closeQuestionModal() {
  document.getElementById('question-modal').classList.add('hidden');
  gameState.currentQuestion = null;
}

async function submitAnswer(answer, onSuccess) {
  try {
    const res = await fetch('/combat/question', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        questionId: gameState.currentQuestion?.id || '',
        answer: answer
      })
    });
    const data = await res.json();

    closeQuestionModal();
    updateUI();

    if (data.combatActive === false) {
      onSuccess();
    } else {
      onSuccess();
    }
  } catch (err) {
    console.error('Failed to submit answer:', err);
    closeQuestionModal();
    onSuccess();
  }
}

function addCombatLog(message) {
  const log = document.getElementById('combat-log');
  const entry = document.createElement('div');
  entry.className = 'combat-entry';
  entry.textContent = `> ${message}`;
  log.prepend(entry);
}

// ============================================================
// SHOP
// ============================================================
async function restAtInn() {
  try {
    const res = await fetch('/shop/rest', { method: 'GET' });
    const data = await res.json();

    if (data.success) {
      gameState.hp = gameState.maxHp;
      updateUI();
      alert(data.message);
    } else {
      alert('Error: ' + data.error);
    }
  } catch (err) {
    console.error('Failed to rest:', err);
  }
}

// ============================================================
// TIME PORTAL
// ============================================================
async function gatherResource(resourceType, event) {
  // Prevent default button behavior
  event.preventDefault();

  // Disable the button immediately
  event.target.disabled = true;

  // Enable the button after 5 seconds
  setTimeout(() => {
    event.target.disabled = false;
    event.target.style.opacity = '1';
  }, 5000);

  try {
    const res = await fetch(`/portal/gather/${resourceType}`, { method: 'GET' });
    const data = await res.json();

    alert(data.message);
  } catch (err) {
    console.error('Failed to gather:', err);
  }
}

async function craftItem(recipeName) {
  try {
    const res = await fetch(`/portal/craft/${recipeName}`, { method: 'POST' });
    const data = await res.json();

    alert(data.message);
  } catch (err) {
    console.error('Failed to craft:', err);
  }
}

// ============================================================
// UI UPDATES
// ============================================================
function updateUI() {
  document.getElementById('player-name-display').textContent = gameState.playerName;
  document.getElementById('hp').textContent = gameState.hp;
  document.getElementById('maxHp').textContent = gameState.maxHp;
  document.getElementById('level').textContent = gameState.level;
  document.getElementById('gold').textContent = gameState.gold;
}

function updateHPBar() {
  const percentage = (gameState.hp / gameState.maxHp) * 100;
  document.querySelector('.hp-bar').style.width = `${percentage}%`;
  document.getElementById('enemy-hp-text').textContent = `${gameState.hp}/${gameState.maxHp}`;
}

function updateEnemyHpBar() {
  const percentage = (gameState.enemyHp / gameState.enemyMaxHp) * 100;
  document.querySelector('.hp-bar').style.width = `${percentage}%`;
  document.getElementById('enemy-hp-text').textContent = `${gameState.enemyHp}/${gameState.enemyMaxHp}`;
}
