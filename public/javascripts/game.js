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
  currentQuestion: null,
  questionAnswered: false,         // Track if we answered this fight's question
  damageMultiplier: 1.0,            // Current damage multiplier for this fight
  nextQuestionIndex: 0,             // Which question from the list to show next
  resources: {
    caffeine: 0,
    chips: 0,
    documentation: 0
  },
  gathering: false            // Prevent spam-clicking gather buttons
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
       // Update equipment damage/armour from server state
      if (data.equipment?.weapon) {
        gameState.equipment.weapon = data.equipment.weapon;
       } else {
        gameState.equipment.weapon = null;
         }
       if (data.equipment?.armour) {
        gameState.equipment.armour = data.equipment.armour;
       } else {
        gameState.equipment.armour = null;
         }
      updateEquipmentStats();
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

         // If navigating to shop, render inventory list
      if (area === 'shop') {
        renderInventory();
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
    console.log('🗡️ Starting battle...');
    const res = await fetch('/combat/start', { method: 'POST' });
    const data = await res.json();
    console.log('⚔️ Battle response:', data);

    if (data.combatActive) {
           // Reset fight state for new combat
      gameState.questionAnswered = false;
      gameState.damageMultiplier = 1.0;
         // Clear stored answer so we fetch fresh questions each fight
      localStorage.removeItem('questionAnswer');

      gameState.combatActive = true;
      gameState.enemyHp = data.enemy.hp;
      gameState.enemyMaxHp = data.enemy.hp;
         // Calculate the next question to show (cycles through the list)
      const qLen = data.questions?.length || 1;
      const nextQIdx = (gameState.nextQuestionIndex || 0) % qLen;

         // Update player HP from server response on new combat
      document.getElementById('hp').textContent = `${data.playerHp}`;
      document.getElementById('maxHp').textContent = `${data.playerMaxHp}`;

         // Reset enemy display to visible (fixes issue where killed enemy stays hidden)
      const enemyDisplay = document.getElementById('enemy-display');
      if (enemyDisplay) enemyDisplay.style.display = 'block';

        // Hide no-combat, show combat area
      document.getElementById('no-combat-area').classList.add('hidden');
      document.getElementById('combat-area').classList.remove('hidden');

        // Clear combat log for fresh battle
      document.getElementById('combat-log').innerHTML = '';

        // Update all enemy UI elements
      document.getElementById('enemy-sprite').src = `/assets/images/${data.enemy.imagePath}`;
      document.getElementById('enemy-name').textContent = data.enemy.name;
      document.getElementById('enemy-level').textContent = `lv ${data.enemy.level}`;
      updateEnemyHpBar();

      console.log('✅ Battle started! Combat active:', gameState.combatActive);
     } else {
      console.error('❌ Failed to start combat:', data);
     }
   } catch (err) {
    console.error('❌ Error starting battle:', err);
   }
}

async function attackEnemy() {
  if (!gameState.combatActive) return;

  console.log('⚔️ Attacking enemy...');

    // If we already answered this fight's question, skip modal and go straight to attack
  if (gameState.questionAnswered) {
    console.log(`✅ Already answered - ${gameState.damageMultiplier}x damage active!`);
    performAttackWithMultiplier(gameState.damageMultiplier);
    return;
   }

    // Otherwise, show the question modal
  showCombatQuestion(() => {
      // After answering/skipping, actually perform the attack
     performAttackFromModal();
    });
}

async function performAttackFromModal() {
  if (!gameState.combatActive) return;

    // Check if we answered (questionAnswered flag)
  if (gameState.questionAnswered) {
       // Answered → 2x damage
    console.log('✅ Answered! Performing attack with 2x multiplier...');
    performAttackWithMultiplier(2.0);
    return;
   }

    // Skipped/answered no → normal damage (don't show modal again)
  console.log('⚔️ Skipping/No answer - Normal damage (no modal next time).');
  gameState.questionAnswered = true; // So we skip modal for rest of fight
  performAttackWithMultiplier(1.0);
}

async function performAttackWithMultiplier(multiplier) {
  if (!gameState.combatActive) return;

  console.log(`⚔️ Performing attack with x${multiplier} multiplier...`);
  try {
    const res = await fetch(`/combat/attack?multiplier=${multiplier}`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({})
         });
    const data = await res.json();
    console.log('⚔️ Attack result:', data);

    addCombatLog(data.message);
    gameState.hp = data.playerHp;
        // Update player HP display
      document.getElementById('hp').textContent = `${gameState.hp}`;
      document.getElementById('maxHp').textContent = `${gameState.maxHp}`;

       // Check if player is defeated (HP reached 0)
   if (data.playerHp <= 0) {
      gameState.combatActive = false;
        // Increment question index for next fight
     gameState.nextQuestionIndex++;
       addCombatLog('💀 You have been defeated!');
       setTimeout(() => {
        document.getElementById('combat-area').classList.add('hidden');
        document.getElementById('no-combat-area').classList.remove('hidden');
          alert('You have been defeated, visit the Catalogue to reserve a spot at the Inn.');
            // Reset combat state so player can fight again
         gameState.enemyHp = 0;
         gameState.questionAnswered = false;
          gameState.damageMultiplier = 1.0;
         }, 1000);
       return;
      }

         // Update enemy HP from server response (fixes stale enemy bar)
    if (data.currentEnemyHp !== undefined) {
      gameState.enemyHp = data.currentEnemyHp;
      updateEnemyHpBar();
     }

if (data.killed) {
      // Enemy killed - update gold and increment question index
  gameState.gold = gameState.gold + data.reward;
  gameState.nextQuestionIndex++;
   updateUI();

        // Reset enemy display for next battle
          const enemyDisplay = document.getElementById('enemy-display');
      if (enemyDisplay) enemyDisplay.style.display = 'block';

          gameState.combatActive = false;
          addCombatLog(`🎉 Enemy defeated! You earned ${data.reward} gold!`);

          // After 1.5s, switch back to no-combat view
          setTimeout(() => {
            document.getElementById('combat-area').classList.add('hidden');
        document.getElementById('no-combat-area').classList.remove('hidden');
            // Reset enemy HP bar for next battle
            const hpBar = document.querySelector('.hp-bar');
      if (hpBar) hpBar.style.width = '0%';
      }, 1500);
        }
       } catch (err) {
    console.error('❌ Attack failed:', err);
  }
}

async function endCombat() {
  try {
    await fetch('/combat/end', { method: 'POST' });
   // Combat ended by running away - increment question index for next fight
    gameState.combatActive = false;
    gameState.questionAnswered = false;
    gameState.damageMultiplier = 1.0;
    gameState.nextQuestionIndex++;
    document.getElementById('combat-area').classList.add('hidden');
    document.getElementById('no-combat-area').classList.remove('hidden');
    addCombatLog('You ran away safely.');
     } catch (err) {
    console.error('Failed to end combat:', err);
       // Still reset on error
    gameState.combatActive = false;
    gameState.questionAnswered = false;
    gameState.damageMultiplier = 1.0;
     }
}

// ============================================================
// COMBAT QUESTIONS (The Retro Game Twist!)
// ============================================================
async function showCombatQuestion(onSuccess) {
   gameState.onAttackSuccess = onSuccess;

  try {
    console.log('❓ Showing combat question...');
       // Check if there's already an answer stored for this question
    let stored = localStorage.getItem('questionAnswer');

    if (stored) {
      const parsed = JSON.parse(stored);
      gameState.currentQuestion = { id: 'q' + Date.now(), question: parsed.question };
      console.log('✅ Previous answer found, showing modal...');
      showQuestionModal(onSuccess, true); // Already answered
      return;
          }

       // Fetch a new question
    const res = await fetch('/questions');
    console.log('📡 Questions response:', res.status, res.statusText);
    const data = await res.json();
    console.log('📋 Questions received:', data);

    if (data.questions.length === 0) {
      addCombatLog("No questions available. Skipping...");
      onSuccess();
      return;
          }

       // Pick question based on cycle index instead of random
    const qLen = data.questions.length;
    const qIndex = gameState.nextQuestionIndex % qLen;
    const questionText = data.questions[qIndex];
    gameState.currentQuestion = { id: 'q' + Date.now(), question: questionText, index: qIndex };
    console.log('❓ Question #' + (qIndex + 1) + ' of ' + qLen + ':', questionText);

    showQuestionModal(onSuccess, false);
     } catch (err) {
    console.error('❌ Failed to get question:', err);
    if (onSuccess) onSuccess();
     }
}

function showQuestionModal(onSuccess, alreadyAnswered) {
  const modal = document.getElementById('question-modal');
  const questionText = document.getElementById('question-text');
  const optionsContainer = document.getElementById('options-container');
  questionText.textContent = gameState.currentQuestion?.question || 'What do you think?';

     // Hide options container (was for old multiple choice buttons)
  optionsContainer.innerHTML = `
     <input type="text" id="answer-input" placeholder="Type your answer here..."
            style="width: 100%; padding: 8px; margin: 10px 0; border: 1px solid #ccc; border-radius: 4px;" />
     <div style="display: flex; gap: 10px; justify-content: center;">
       <button onclick="submitAttackAnswer()" class="action-btn primary">Submit</button>
       <button onclick="skipAttackQuestion()" class="action-btn secondary">Skip</button>
     </div>
   `;

  modal.classList.remove('hidden');
}

async function submitAttackAnswer() {
  try {
    const input = document.getElementById('answer-input');
    const answer = input ? input.value.trim() : '';

       // Mark as answered and set 2x damage for this fight
    gameState.questionAnswered = true;
    gameState.damageMultiplier = 2.0;

   // Store questions as an array of { question, answer } objects
   let stored = localStorage.getItem('questions');
   let questionsArray = stored ? JSON.parse(stored) : [];
   questionsArray.push({
       question: gameState.currentQuestion?.question || 'Unknown question',
       answer: answer
   });
   localStorage.setItem('questions', JSON.stringify(questionsArray));

   // Also store current answer for flow control
   localStorage.setItem('questionAnswer', JSON.stringify({
     question: gameState.currentQuestion?.question,
     answer: answer,
     id: 'q' + Date.now()
         }));

    closeQuestionModal();

    if (gameState.onAttackSuccess) {
          // Perform attack with the 2x multiplier
      performAttackWithMultiplier(2.0);
         }
       } catch (err) {
    console.error('Failed to submit answer:', err);
    closeQuestionModal();
    if (gameState.onAttackSuccess) {
      performAttackWithMultiplier(2.0);
         }
  }
}

function skipAttackQuestion() {
  try {
       // Don't mark as answered - user skipped, so normal damage only
    gameState.questionAnswered = true;
    gameState.damageMultiplier = 1.0;

    closeQuestionModal();

    if (gameState.onAttackSuccess) {
          // Perform attack with normal damage
      performAttackWithMultiplier(1.0);
         }
       } catch (err) {
    console.error('Failed to skip question:', err);
    closeQuestionModal();
    if (gameState.onAttackSuccess) {
      performAttackWithMultiplier(1.0);
         }
       }
}

function closeQuestionModal() {
  document.getElementById('question-modal').classList.add('hidden');
  gameState.currentQuestion = null;
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
      if (data.gold !== undefined) gameState.gold = data.gold;
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
  // Prevent default and stop propagation immediately
  event.preventDefault();
  event.stopPropagation();

  // Block spam-clicking - only allow one gather at a time
  if (gameState.gathering) return;
  gameState.gathering = true;

  // Disable the button that was clicked
  const btn = event.currentTarget;
  btn.disabled = true;
  btn.style.opacity = '0.5';

  try {
    const res = await fetch(`/portal/gather/${resourceType}`, { method: 'GET' });
    const data = await res.json();

    // Refetch full player state to get updated resource counts
    await fetchPlayerState();

    alert(data.message);
  } catch (err) {
    console.error('Failed to gather:', err);
    alert('Something went wrong while gathering resources.');
  } finally {
    // Re-enable the button after cooldown period
    setTimeout(() => {
      btn.disabled = false;
      btn.style.opacity = '1';
      gameState.gathering = false;
    }, 3000);
  }
}

async function craftItem(recipeName) {
  try {
    const res = await fetch(`/portal/craft/${recipeName}`, { method: 'POST' });
    const data = await res.json();

    if (data.success) {
      // Refetch player state to get updated inventory and resource counts
      await fetchPlayerState();
      renderInventory();
      alert(data.message);
     } else {
      alert(data.message);
     }
   } catch (err) {
    console.error('Failed to craft:', err);
    alert('Something went wrong while crafting.');
   }
}

// ============================================================
// UI UPDATES
// ============================================================
function updateUI() {
  document.getElementById('player-name-display').textContent = gameState.playerName;
  document.getElementById('hp').textContent = gameState.hp;
  document.getElementById('maxHp').textContent = gameState.maxHp;
  document.getElementById('gold').textContent = gameState.gold;

    // Update resource counters from game state
  updateResourceCount('caffeineResources', gameState.resources.caffeine);
  updateResourceCount('chipsResources', gameState.resources.chips);
  updateResourceCount('documentationResources', gameState.resources.documentation);

    // Show equipment stats
  updateEquipmentStats();
}

function updateEquipmentStats() {
  var dmg = 0;
  if (gameState.equipment?.weapon) {
    dmg = gameState.equipment.weapon.damage || 0;
     }
  document.getElementById('damage').textContent = dmg;

  var arm = 0;
  if (gameState.equipment?.armour) {
    arm = gameState.equipment.armour.armour || 0;
     }
  document.getElementById('armour').textContent = arm;
}

function updateResourceCount(elementId, count) {
  const element = document.getElementById(elementId);
  if (!element) return;

  const iconMap = {
    'caffeineResources': '🌿',
    'chipsResources': '📟',
    'documentationResources': '📑'
  };
  const icon = iconMap[elementId] || '❓';
  element.textContent = `${icon}: ${count}`;
}

// ============================================================
// INVENTORY RENDERING
// ============================================================
function renderInventory() {
  const list = document.getElementById('inventory-list');
  if (!list) return;

  if (gameState.inventory.length === 0) {
    list.innerHTML = '<li class="empty-inventory">Your inventory is empty. Craft some items!</li>';
    return;
  }

  // Group items by name and count them for cleaner display
  const counts = {};
  gameState.inventory.forEach(item => {
    if (!['caffeine', 'chips', 'documentation'].includes(item)) {
      counts[item] = (counts[item] || 0) + 1;
    }
  });

  list.innerHTML = Object.entries(counts)
        .map(([item, count]) => {
          const safeName = item.replace(/'/g, "\\'");
          return `<li>${count}x ${item} <button class="action-btn secondary sell-btn" onclick="sellItem('${safeName}')">Sell</button></li>`;
        })
        .join('');
}

// ============================================================
// SELL ITEMS
// ============================================================
async function sellItem(itemName) {
  try {
    const res = await fetch('/shop/sell/' + encodeURIComponent(itemName), { method: 'POST' });
    const data = await res.json();

    if (data.success) {
      // Get fresh inventory from server
      await fetchPlayerState();
      updateUI();
      renderInventory();
      alert(data.message);
    } else {
      alert('Error: ' + data.error);
    }
  } catch (err) {
    console.error('Failed to sell:', err);
    alert('Something went wrong while selling.');
  }
}

function updateHPBar() {
   // Player HP shown in header, not combat hp-bar
  document.getElementById('hp').textContent = `${gameState.hp}`;
  document.getElementById('maxHp').textContent = `${gameState.maxHp}`;
}

function updateEnemyHpBar() {
  const percentage = (gameState.enemyHp / gameState.enemyMaxHp) * 100;
  document.querySelector('.hp-bar').style.width = `${percentage}%`;
  document.getElementById('enemy-hp-text').textContent = `${gameState.enemyHp}/${gameState.enemyMaxHp}`;
}

// ============================================================
// END GAME
// ============================================================
async function endGame() {
  // Show loading state
  const qaList = document.getElementById('questions-answers-list');
  const statsDiv = document.getElementById('endgame-stats');

  // Fetch fresh player state from server
  try {
    const res = await fetch('/player/state');
    const data = await res.json();

    gameState.playerName = data.playerName || 'Player';
    gameState.gold = data.gold || 0;
    gameState.hp = data.hp || 0;
    gameState.maxHp = data.maxHp || 0;
    gameState.inventory = data.inventory || [];
    gameState.equipment = data.equipment || {};

    // Render questions & answers from localStorage
    renderQuestionsAnswers();

    // Render stats
    renderStats(data);

    // Hide all other views, show endgame view
    document.querySelectorAll('.area-view').forEach(view => {
      view.classList.add('hidden');
    });
    document.getElementById('endgame-view').classList.remove('hidden');
  } catch (err) {
    console.error('Failed to load player state for end game:', err);
    qaList.innerHTML = '<p class="no-answers">Failed to load your stats.</p>';
    renderQuestionsAnswers();

    document.querySelectorAll('.area-view').forEach(view => {
      view.classList.add('hidden');
    });
    document.getElementById('endgame-view').classList.remove('hidden');
  }
}

function renderQuestionsAnswers() {
  const qaList = document.getElementById('questions-answers-list');
  const stored = localStorage.getItem('questions');

  if (!stored) {
    qaList.innerHTML = '<p class="no-answers">No questions were answered during your adventure.</p>';
    return;
    }

  try {
    const questionsArray = JSON.parse(stored);
    
    if (!Array.isArray(questionsArray) || questionsArray.length === 0) {
      qaList.innerHTML = '<p class="no-answers">No questions were answered during your adventure.</p>';
      return;
      }

    const validEntries = questionsArray.filter(entry => entry && entry.question);
    
    if (validEntries.length === 0) {
      qaList.innerHTML = '<p class="no-answers">No questions were answered during your adventure.</p>';
      return;
      }

    qaList.innerHTML = validEntries.map(entry => `
         <div class="qa-entry">
             <div class="qa-question">❓ ${escapeHtml(entry.question)}</div>
             <div class="qa-answer">${escapeHtml(entry.answer || 'No answer provided')}</div>
         </div>
     `).join('');
    } catch (err) {
    console.error('Failed to parse questions data:', err);
    qaList.innerHTML = '<p class="no-answers">Could not display your answers.</p>';
    }
}

function renderStats(data) {
  const statsDiv = document.getElementById('endgame-stats');

  const equipment = data.equipment || {};
  let attackDamage = 0;
  if (equipment.weapon && equipment.weapon.damage) {
    attackDamage = equipment.weapon.damage;
  }
  let defenseArmour = 0;
  if (equipment.armour && equipment.armour.armour) {
    defenseArmour = equipment.armour.armour;
  }

  const inventoryList = data.inventory || [];
  const uniqueItems = {};
  inventoryList.forEach(item => {
    if (!['caffeine', 'chips', 'documentation'].includes(item)) {
      uniqueItems[item] = (uniqueItems[item] || 0) + 1;
    }
  });

  let inventoryHtml = '';
  if (Object.keys(uniqueItems).length === 0) {
    inventoryHtml = '<span class="stat-value">Empty</span>';
  } else {
    inventoryHtml = Object.entries(uniqueItems)
      .map(([item, count]) => `${count}x ${item}`)
      .join(', ');
  }

  statsDiv.innerHTML = `
    <div class="stat-row">
      <span class="stat-label">Player</span>
      <span class="stat-value">${escapeHtml(data.playerName || 'Player')}</span>
    </div>
    <div class="stat-row">
      <span class="stat-label">HP</span>
      <span class="stat-value">${data.hp || 0} / ${data.maxHp || 0}</span>
    </div>
    <div class="stat-row">
      <span class="stat-label">Gold</span>
      <span class="stat-value">💰 ${data.gold || 0}</span>
    </div>
    <div class="stat-row">
      <span class="stat-label">Attack Damage</span>
      <span class="stat-value">⚔️ ${attackDamage}</span>
    </div>
    <div class="stat-row">
      <span class="stat-label">Defence Armour</span>
      <span class="stat-value">🛡️ ${defenseArmour}</span>
    </div>
    <div class="stat-row">
      <span class="stat-label">Inventory</span>
      <span class="stat-value">${inventoryHtml}</span>
    </div>
  `;
}

function escapeHtml(text) {
  const div = document.createElement('div');
  div.textContent = text;
  return div.innerHTML;
}
