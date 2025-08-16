// === ELEMENTOS DEL DOM ===
const configPanel = document.getElementById('config-panel');
const timerDisplay = document.getElementById('timer-display');
const completionScreen = document.getElementById('completion-screen');
const setTimeBtn = document.getElementById('set-time-btn');
const newSessionBtn = document.getElementById('new-session-btn');

// Inputs de configuración
const totalHoursInput = document.getElementById('total-hours');
const workMinutesInput = document.getElementById('work-minutes');
const shortBreakMinutesInput = document.getElementById('short-break-minutes');
const longBreakMinutesInput = document.getElementById('long-break-minutes');
const pomodorosUntilLongInput = document.getElementById('pomodoros-until-long');

// Elementos del timer
const timerLabel = document.getElementById('timer-label');
const sessionType = document.getElementById('session-type');
const sessionProgress = document.getElementById('session-progress');
const beaker = document.getElementById('beaker-container');
const liquid = document.getElementById('liquid');
const vaporEffect = document.getElementById('vapor-effect');
const bubblesEffect = document.getElementById('bubbles-effect');

// Elementos de progreso visual
const progressShelf = document.getElementById('progress-shelf');
const testTubesContainer = document.getElementById('test-tubes-container');

// Controles
const pauseBtn = document.getElementById('pause-btn');
const resetBtn = document.getElementById('reset-btn');
const skipBtn = document.getElementById('skip-btn');
const pipBtn = document.getElementById('pip-btn');
const pauseBtnText = document.querySelector('#pause-btn span');

// Elementos de audio
const soundStart = document.getElementById('sound-start');
const soundShortBreak = document.getElementById('sound-short-break');
const soundLongBreak = document.getElementById('sound-long-break');

// === VARIABLES GLOBALES DEL GESTOR DE CICLOS ===
let sessionPlan = []; // Array con el plan completo: ['TRABAJO', 'CORTO', 'TRABAJO', 'LARGO', ...]
let currentSessionIndex = 0; // Índice de la sesión actual en el plan
let totalSessions = 0; // Total de sesiones en el plan
let workSessionsCompleted = 0; // Contador de sesiones de trabajo completadas

// Variables del temporizador actual
let timerInterval;
let totalSeconds;
let remainingSeconds;
let isRunning = false;
let isPaused = false;
let currentLiquidHue;

// Variables para efectos visuales
let pipBubbles = [];
let wakeLock = null;
let vaporInterval = null;

// Configuración de la sesión
let config = {
    totalHours: 2,
    workMinutes: 25,
    shortBreakMinutes: 5,
    longBreakMinutes: 15,
    pomodorosUntilLong: 4
};

// === FUNCIÓN PARA AGREGAR ESTILOS ADICIONALES ===
function agregarEstilosAdicionales() {
    // Verificar si ya se agregaron los estilos
    if (document.getElementById('estilos-timer-adicionales')) {
        return; // Ya existen, no duplicar
    }
    
    const additionalCSS = `
/* Estilos para el estante de probetas */
.test-tube {
    transition: all 0.3s ease;
    position: relative;
    overflow: hidden;
}

.test-tube.in-progress {
    animation: pulse-glow 2s infinite;
    box-shadow: 0 0 10px rgba(52, 152, 219, 0.5);
}

.test-tube.completed {
    opacity: 0.8;
}

.test-tube.work-completed {
    box-shadow: 0 0 15px rgba(241, 196, 15, 0.7);
}

.test-tube.break-completed {
    box-shadow: 0 0 15px rgba(46, 204, 113, 0.7);
}

@keyframes pulse-glow {
    0%, 100% { box-shadow: 0 0 5px rgba(52, 152, 219, 0.3); }
    50% { box-shadow: 0 0 20px rgba(52, 152, 219, 0.8); }
}

/* Estilos para partículas de vapor */
.vapor-particle {
    position: absolute;
    width: 4px;
    height: 4px;
    background: rgba(255, 255, 255, 0.7);
    border-radius: 50%;
    animation: vapor-rise linear infinite;
    pointer-events: none;
    z-index: 10;
}

@keyframes vapor-rise {
    0% {
        transform: translateY(0) scale(0.5);
        opacity: 0.8;
    }
    50% {
        transform: translateY(-30px) scale(1);
        opacity: 0.6;
    }
    100% {
        transform: translateY(-80px) scale(1.5);
        opacity: 0;
    }
}

/* Mejorar el efecto de vapor existente */
#vapor-effect.active {
    opacity: 1;
    animation: vapor-flow 4s infinite;
}

@keyframes vapor-flow {
    0%, 100% { 
        transform: translateY(0) scale(1);
        opacity: 0.6;
    }
    50% { 
        transform: translateY(-10px) scale(1.1);
        opacity: 0.8;
    }
}

/* Animación para el líquido que se vacía */
#liquid {
    transition: height 1s ease-out;
}

/* Mejoras para el contenedor de vapor */
#vapor-effect {
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    height: 100px;
    pointer-events: none;
    z-index: 5;
}
`;

    // Crear elemento style y agregarlo al head
    const styleSheet = document.createElement('style');
    styleSheet.id = 'estilos-timer-adicionales';
    styleSheet.textContent = additionalCSS;
    document.head.appendChild(styleSheet);
    
    console.log('Estilos adicionales agregados correctamente');
}

// === FUNCIÓN PRINCIPAL: GENERAR PLAN DE TRABAJO ===
function generarPlan() {
    try {
        const totalHours = parseInt(totalHoursInput.value, 10);
        const workMinutes = parseInt(workMinutesInput.value, 10);
        const shortBreakMinutes = parseInt(shortBreakMinutesInput.value, 10);
        const longBreakMinutes = parseInt(longBreakMinutesInput.value, 10);
        const pomodorosUntilLong = parseInt(pomodorosUntilLongInput.value, 10);

        // Validaciones mejoradas
        if (isNaN(totalHours) || totalHours < 1 || totalHours > 24) {
            alert("Las horas deben estar entre 1 y 24.");
            return false;
        }
        
        if (isNaN(workMinutes) || workMinutes < 5 || workMinutes > 120) {
            alert("Los minutos de trabajo deben estar entre 5 y 120.");
            return false;
        }
        
        if (isNaN(shortBreakMinutes) || shortBreakMinutes < 1 || shortBreakMinutes > 60) {
            alert("Los minutos de descanso corto deben estar entre 1 y 60.");
            return false;
        }
        
        if (isNaN(longBreakMinutes) || longBreakMinutes < 5 || longBreakMinutes > 120) {
            alert("Los minutos de descanso largo deben estar entre 5 y 120.");
            return false;
        }
        
        if (isNaN(pomodorosUntilLong) || pomodorosUntilLong < 2 || pomodorosUntilLong > 10) {
            alert("Los pomodoros hasta descanso largo deben estar entre 2 y 10.");
            return false;
        }

        // Guardar configuración
        config = {
            totalHours,
            workMinutes,
            shortBreakMinutes,
            longBreakMinutes,
            pomodorosUntilLong
        };

        // Calcular número total de pomodoros necesarios
        const totalWorkTimeMinutes = totalHours * 60;
        const totalPomodoros = Math.ceil(totalWorkTimeMinutes / workMinutes);
        
        // Generar el plan
        sessionPlan = [];
        let pomodoroCount = 0;

        for (let i = 0; i < totalPomodoros; i++) {
            sessionPlan.push('TRABAJO');
            pomodoroCount++;
            
            // Solo agregar descanso si no es el último pomodoro
            if (i < totalPomodoros - 1) {
                if (pomodoroCount % pomodorosUntilLong === 0) {
                    sessionPlan.push('LARGO');
                } else {
                    sessionPlan.push('CORTO');
                }
            }
        }

        totalSessions = sessionPlan.length;
        currentSessionIndex = 0;
        workSessionsCompleted = 0;

        console.log('Plan generado:', sessionPlan);
        console.log('Total sesiones:', totalSessions);
        return true;
        
    } catch (error) {
        console.error('Error generando plan:', error);
        alert("Error al generar el plan. Por favor, verifica los valores ingresados.");
        return false;
    }
}

// === FUNCIÓN: CREAR REPISA VISUAL (MEJORADA) ===
function crearRepisaVisual() {
    testTubesContainer.innerHTML = '';
    
    sessionPlan.forEach((sessionType, index) => {
        const testTube = document.createElement('div');
        testTube.className = `test-tube ${sessionType.toLowerCase().replace(' ', '-')}`;
        testTube.setAttribute('data-session', index);
        
        const liquidDiv = document.createElement('div');
        liquidDiv.className = 'test-tube-liquid';
        
        // Inicializar todas las probetas vacías
        liquidDiv.style.height = '0%';
        
        // Asignar colores según el tipo de sesión
        if (sessionType === 'TRABAJO') {
            liquidDiv.style.backgroundColor = 'hsl(' + Math.floor(Math.random() * 360) + ', 80%, 60%)';
        } else if (sessionType === 'CORTO') {
            liquidDiv.style.backgroundColor = '#2ecc71'; // Verde para descanso corto
        } else if (sessionType === 'LARGO') {
            liquidDiv.style.backgroundColor = '#e74c3c'; // Rojo para descanso largo
        }
        
        testTube.appendChild(liquidDiv);
        testTubesContainer.appendChild(testTube);
    });
    
    progressShelf.classList.remove('hidden');
}

// === FUNCIÓN: ACTUALIZAR PROGRESO VISUAL DEL ESTANTE ===
function actualizarProgresoVisualEstante() {
    // Actualizar solo la sesión actual
    const currentTube = testTubesContainer.children[currentSessionIndex];
    if (currentTube) {
        const liquidDiv = currentTube.querySelector('.test-tube-liquid');
        const sessionType = sessionPlan[currentSessionIndex];
        
        if (sessionType === 'TRABAJO') {
            // En trabajo: el líquido se llena progresivamente (representando acumulación de trabajo)
            const percentage = ((totalSeconds - remainingSeconds) / totalSeconds) * 100;
            liquidDiv.style.height = `${percentage}%`;
        } else {
            // En descanso: el líquido se llena progresivamente (representando relajación)
            const percentage = ((totalSeconds - remainingSeconds) / totalSeconds) * 100;
            liquidDiv.style.height = `${percentage}%`;
        }
        
        // Añadir clase de progreso para animaciones
        currentTube.classList.add('in-progress');
    }
}

// === FUNCIÓN: MARCAR SESIÓN COMO COMPLETADA (MEJORADA) ===
function marcarSesionCompletada() {
    const completedTube = testTubesContainer.children[currentSessionIndex];
    if (completedTube) {
        const liquidDiv = completedTube.querySelector('.test-tube-liquid');
        const sessionType = sessionPlan[currentSessionIndex];
        
        // Remover clase de progreso y añadir clase de completado
        completedTube.classList.remove('in-progress');
        completedTube.classList.add('completed');
        
        if (sessionType === 'TRABAJO') {
            // Trabajo completado: tubo lleno al 100%
            liquidDiv.style.height = '100%';
            workSessionsCompleted++;
            
            // Añadir efecto de brillo para trabajo completado
            completedTube.classList.add('work-completed');
        } else {
            // Descanso completado: tubo lleno al 100%
            liquidDiv.style.height = '100%';
            
            // Añadir efecto de relajación para descanso completado
            completedTube.classList.add('break-completed');
        }
        
        // Efecto de pulso al completar
        setTimeout(() => {
            completedTube.style.transform = 'scale(1.1)';
            setTimeout(() => {
                completedTube.style.transform = 'scale(1)';
            }, 200);
        }, 100);
    }
}

// === FUNCIÓN: CREAR EFECTO DE VAPOR ===
function crearEfectoVapor() {
    const vaporContainer = document.getElementById('vapor-effect');
    if (!vaporContainer) return;
    
    // Crear múltiples partículas de vapor
    for (let i = 0; i < 8; i++) {
        const vaporParticle = document.createElement('div');
        vaporParticle.className = 'vapor-particle';
        
        // Posición inicial aleatoria en la parte superior de la probeta
        vaporParticle.style.left = `${45 + Math.random() * 10}%`;
        vaporParticle.style.animationDelay = `${Math.random() * 2}s`;
        vaporParticle.style.animationDuration = `${2 + Math.random() * 2}s`;
        
        vaporContainer.appendChild(vaporParticle);
        
        // Remover la partícula después de la animación
        setTimeout(() => {
            if (vaporParticle.parentNode) {
                vaporParticle.remove();
            }
        }, 4000);
    }
}

// === FUNCIÓN: CONFIGURAR EFECTOS VISUALES (MEJORADA) ===
function configurarEfectosVisuales(sessionType) {
    try {
        console.log('Configurando efectos para:', sessionType);
        
        // Limpiar efectos anteriores
        vaporEffect.classList.remove('active');
        bubblesEffect.classList.remove('active');
        
        // Limpiar interval anterior de vapor
        if (vaporInterval) {
            clearInterval(vaporInterval);
            vaporInterval = null;
        }
        
        if (sessionType === 'TRABAJO') {
            // Activar efecto de vapor para trabajo
            vaporEffect.classList.add('active');
            setRandomLiquidColor();
            
            // Crear vapor continuo durante el trabajo
            vaporInterval = setInterval(() => {
                if (isRunning && sessionPlan[currentSessionIndex] === 'TRABAJO') {
                    crearEfectoVapor();
                } else {
                    clearInterval(vaporInterval);
                    vaporInterval = null;
                }
            }, 3000);
            
            console.log('Efecto de vapor activado');
        } else {
            // Activar efecto de burbujas para descansos
            bubblesEffect.classList.add('active');
            setBreakLiquidColor(sessionType);
            console.log('Efecto de burbujas activado para', sessionType);
        }
    } catch (error) {
        console.error('Error configurando efectos visuales:', error);
    }
}

// === FUNCIÓN: COLORES DEL LÍQUIDO ===
function setRandomLiquidColor() {
    currentLiquidHue = Math.floor(Math.random() * 360);
    const newColor = `hsl(${currentLiquidHue}, 80%, 60%)`;
    liquid.style.backgroundColor = newColor;
}

function setBreakLiquidColor(sessionType) {
    if (sessionType === 'CORTO') {
        currentLiquidHue = 120; // Verde para descanso corto
        liquid.style.backgroundColor = '#2ecc71';
    } else if (sessionType === 'LARGO') {
        currentLiquidHue = 0; // Rojo para descanso largo
        liquid.style.backgroundColor = '#e74c3c';
    }
}

// === FUNCIÓN: REPRODUCIR SONIDOS ===
function reproducirSonido(tipo) {
    try {
        let audio;
        switch (tipo) {
            case 'inicio':
                audio = soundStart;
                break;
            case 'descanso-corto':
                audio = soundShortBreak;
                break;
            case 'descanso-largo':
                audio = soundLongBreak;
                break;
            default:
                return;
        }
        
        audio.currentTime = 0;
        audio.play().catch(err => {
            console.log('No se pudo reproducir el sonido:', err);
        });
    } catch (error) {
        console.log('Error al reproducir sonido:', error);
    }
}

// === FUNCIÓN PRINCIPAL: INICIAR SIGUIENTE SESIÓN ===
async function iniciarSiguienteSesion() {
    try {
        console.log(`Iniciando sesión ${currentSessionIndex + 1} de ${totalSessions}`);
        
        if (currentSessionIndex >= sessionPlan.length) {
            console.log('Todas las sesiones completadas');
            mostrarPantallaFinalizacion();
            return;
        }

        const sessionType = sessionPlan[currentSessionIndex];
        console.log('Tipo de sesión:', sessionType);
        
        // Configurar duración según el tipo de sesión
        switch (sessionType) {
            case 'TRABAJO':
                totalSeconds = config.workMinutes * 60;
                break;
            case 'CORTO':
                totalSeconds = config.shortBreakMinutes * 60;
                break;
            case 'LARGO':
                totalSeconds = config.longBreakMinutes * 60;
                break;
            default:
                console.error('Tipo de sesión desconocido:', sessionType);
                totalSeconds = 25 * 60; // Fallback
        }
        
        remainingSeconds = totalSeconds;
        console.log('Duración configurada:', totalSeconds, 'segundos');
        
        // Actualizar UI
        actualizarInformacionSesion();
        configurarEfectosVisuales(sessionType);
        updateUI();
        
        // Reproducir sonido de inicio
        reproducirSonido('inicio');
        
        // Activar Wake Lock
        await requestWakeLock();
        
        // Iniciar temporizador
        startTimer();
        
    } catch (error) {
        console.error('Error en iniciarSiguienteSesion:', error);
        alert('Error al iniciar la sesión. Continuando...');
    }
}

// === FUNCIÓN: ACTUALIZAR INFORMACIÓN DE SESIÓN ===
function actualizarInformacionSesion() {
    const currentSessionType = sessionPlan[currentSessionIndex];
    const sessionNumber = currentSessionIndex + 1;
    
    // Actualizar tipo de sesión
    switch (currentSessionType) {
        case 'TRABAJO':
            sessionType.textContent = '🍅 Tiempo de Trabajo';
            break;
        case 'CORTO':
            sessionType.textContent = '☕ Descanso Corto';
            break;
        case 'LARGO':
            sessionType.textContent = '🌟 Descanso Largo';
            break;
    }
    
    // Actualizar progreso
    sessionProgress.textContent = `Sesión ${sessionNumber} de ${totalSessions}`;
}

// === FUNCIÓN: MOSTRAR PANTALLA DE FINALIZACIÓN ===
function mostrarPantallaFinalizacion() {
    releaseWakeLock();
    
    // Limpiar efectos
    if (vaporInterval) {
        clearInterval(vaporInterval);
        vaporInterval = null;
    }
    
    // Ocultar timer y mostrar pantalla de finalización
    timerDisplay.classList.add('hidden');
    completionScreen.classList.remove('hidden');
    
    // Actualizar resumen
    const completionSummary = document.getElementById('completion-summary');
    const totalWorkTime = workSessionsCompleted * config.workMinutes;
    const hours = Math.floor(totalWorkTime / 60);
    const minutes = totalWorkTime % 60;
    
    completionSummary.textContent = `¡Excelente trabajo! Has completado ${workSessionsCompleted} sesiones de trabajo, totalizando ${hours}h ${minutes}m de productividad.`;
    
    // Reproducir sonido de finalización (usando el sonido largo)
    reproducirSonido('descanso-largo');
    
    // Vibración de celebración
    if ('vibrate' in navigator) {
        navigator.vibrate([500, 200, 500, 200, 500, 200, 1000]);
    }
}

// === FUNCIONES DEL TEMPORIZADOR ===
async function setupTimer() {
    try {
        console.log('Iniciando setup del timer...');
        
        if (!generarPlan()) {
            console.log('Error al generar plan');
            return;
        }
        
        console.log('Plan generado exitosamente');
        
        // Cambiar a vista del timer
        configPanel.classList.add('hidden');
        timerDisplay.classList.remove('hidden');
        
        // Crear repisa visual
        crearRepisaVisual();
        console.log('Repisa visual creada');
        
        // Iniciar primera sesión
        await iniciarSiguienteSesion();
        console.log('Primera sesión iniciada');
        
    } catch (error) {
        console.error('Error en setupTimer:', error);
        alert('Error al iniciar la sesión. Por favor, intenta de nuevo.');
        
        // Volver al panel de configuración si hay error
        timerDisplay.classList.add('hidden');
        configPanel.classList.remove('hidden');
    }
}

async function startTimer() {
    if (isRunning) return;
    
    isRunning = true;
    isPaused = false;
    pauseBtnText.textContent = "Pausar";
    
    if (!wakeLock) {
        await requestWakeLock();
    }
    
    timerInterval = setInterval(() => {
        remainingSeconds--;
        updateUI(); // Esta función ya incluye actualizarProgresoVisualEstante()
        
        // Crear burbujas según el tipo de sesión
        if (isRunning && sessionPlan[currentSessionIndex]) {
            const sessionType = sessionPlan[currentSessionIndex];
            if (sessionType === 'CORTO' || sessionType === 'LARGO') {
                createBubble();
            }
        }
        
        if (remainingSeconds <= 0) {
            clearInterval(timerInterval);
            isRunning = false;
            releaseWakeLock();
            
            // Marcar sesión como completada (ahora llena las probetas correctamente)
            marcarSesionCompletada();
            
            // Reproducir sonido según el tipo de sesión que termina
            const sessionType = sessionPlan[currentSessionIndex];
            if (sessionType === 'TRABAJO') {
                reproducirSonido('descanso-corto');
            } else if (sessionType === 'LARGO') {
                reproducirSonido('descanso-largo');
            } else {
                reproducirSonido('descanso-corto');
            }
            
            // Vibración
            if ('vibrate' in navigator) {
                navigator.vibrate([500, 200, 500, 200, 500]);
            }
            
            // Avanzar a la siguiente sesión
            currentSessionIndex++;
            
            // Pausa de 3 segundos antes de la siguiente sesión
            setTimeout(async () => {
                await iniciarSiguienteSesion();
            }, 3000);
        }
    }, 1000);
}

function pauseTimer() {
    isRunning = false;
    isPaused = true;
    clearInterval(timerInterval);
    releaseWakeLock();
    pauseBtnText.textContent = "Continuar";
    
    // Pausar también el vapor
    if (vaporInterval) {
        clearInterval(vaporInterval);
        vaporInterval = null;
    }
}

function resetTimer() {
    pauseTimer();
    releaseWakeLock();
    
    // Limpiar efectos
    if (vaporInterval) {
        clearInterval(vaporInterval);
        vaporInterval = null;
    }
    
    // Reiniciar variables
    sessionPlan = [];
    currentSessionIndex = 0;
    workSessionsCompleted = 0;
    
    // Limpiar efectos
    vaporEffect.classList.remove('active');
    bubblesEffect.classList.remove('active');
    
    // Volver al panel de configuración
    timerDisplay.classList.add('hidden');
    completionScreen.classList.add('hidden');
    progressShelf.classList.add('hidden');
    configPanel.classList.remove('hidden');
}

function skipSession() {
    if (!isRunning && !isPaused) return;
    
    // Parar el timer actual
    clearInterval(timerInterval);
    isRunning = false;
    isPaused = false;
    
    // Marcar como completada y avanzar
    marcarSesionCompletada();
    currentSessionIndex++;
    
    // Continuar con la siguiente sesión
    setTimeout(async () => {
        await iniciarSiguienteSesion();
    }, 1000);
}

// === FUNCIÓN: ACTUALIZAR UI (MEJORADA CON VAPOR) ===
function updateUI() {
    const sessionType = sessionPlan[currentSessionIndex];
    
    if (sessionType === 'TRABAJO') {
        // En trabajo: líquido se vacía (evaporación)
        const percentage = (remainingSeconds / totalSeconds) * 100;
        liquid.style.height = `${percentage}%`;
        
        // Intensificar vapor cuando queda poco líquido
        if (percentage < 20 && isRunning) {
            crearEfectoVapor();
        }
    } else {
        // En descanso: líquido se llena
        const percentage = ((totalSeconds - remainingSeconds) / totalSeconds) * 100;
        liquid.style.height = `${percentage}%`;
    }
    
    const mins = Math.floor(remainingSeconds / 60);
    const secs = remainingSeconds % 60;
    timerLabel.textContent = `${String(mins).padStart(2, '0')}:${String(secs).padStart(2, '0')}`;
    
    // Actualizar estante de probetas
    actualizarProgresoVisualEstante();
}

function createBubble() {
    try {
        if (!sessionPlan[currentSessionIndex]) return;
        
        const sessionType = sessionPlan[currentSessionIndex];
        console.log('Creando burbuja para sesión:', sessionType);
        
        const bubble = document.createElement('div');
        bubble.classList.add('bubble');
        const contrastingHue = (currentLiquidHue + 180) % 360;
        const bubbleColor = `hsla(${contrastingHue}, 90%, 70%, 0.7)`;
        bubble.style.backgroundColor = bubbleColor;
        const size = Math.random() * 10 + 5;
        bubble.style.width = `${size}px`;
        bubble.style.height = `${size}px`;
        bubble.style.left = `${Math.random() * 90}%`;
        bubble.style.animationDuration = `${Math.random() * 2 + 3}s`;
        
        if (beaker) {
            beaker.appendChild(bubble);
            console.log('Burbuja añadida al beaker');
            setTimeout(() => {
                if (bubble && bubble.parentNode) {
                    bubble.remove();
                }
            }, 5000);
        } else {
            console.error('Beaker no encontrado');
        }
    } catch (error) {
        console.error('Error creando burbuja:', error);
    }
}

// === FUNCIONES WAKE LOCK ===
async function requestWakeLock() {
    if ('wakeLock' in navigator) {
        try {
            wakeLock = await navigator.wakeLock.request('screen');
            console.log('Wake Lock activado');
            
            wakeLock.addEventListener('release', () => {
                console.log('Wake Lock liberado');
                if (isRunning) {
                    setTimeout(requestWakeLock, 1000);
                }
            });
            
        } catch (err) {
            console.log('No se pudo activar Wake Lock:', err.message);
        }
    }
}

function releaseWakeLock() {
    if (wakeLock) {
        wakeLock.release();
        wakeLock = null;
        console.log('Wake Lock liberado manualmente');
    }
}

document.addEventListener('visibilitychange', async () => {
    if (!document.hidden && isRunning && !wakeLock) {
        await requestWakeLock();
    }
});

// === FUNCIÓN PICTURE-IN-PICTURE CORREGIDA ===
async function activarPictureInPicture() {
    if (!document.pictureInPictureEnabled) {
        alert('Tu navegador no soporta el modo Picture-in-Picture.');
        return;
    }

    try {
        if (document.pictureInPictureElement) {
            await document.exitPictureInPicture();
            return;
        }

        const canvas = document.getElementById('pip-canvas') || document.createElement('canvas');
        canvas.id = 'pip-canvas';
        canvas.style.display = 'none';
        if (!document.body.contains(canvas)) {
            document.body.appendChild(canvas);
        }
        
        const ctx = canvas.getContext('2d');
        const video = document.createElement('video');

        video.width = 150;
        video.height = 300;
        video.muted = true;
        video.style.display = 'none';
        document.body.appendChild(video);

        const createPipBubble = () => {
            if (!sessionPlan[currentSessionIndex]) return;
            const sessionType = sessionPlan[currentSessionIndex];
            if (!isRunning || sessionType === 'TRABAJO') return;
            
            const bubble = {
                x: Math.random() * (video.width - 40) + 20,
                y: video.height - 20,
                size: Math.random() * 8 + 4,
                speed: Math.random() * 2 + 1,
                opacity: 1,
                life: 0,
                wobble: Math.random() * Math.PI * 2
            };
            pipBubbles.push(bubble);
        };

        const drawFrame = () => {
            const canvasWidth = video.width;
            const canvasHeight = video.height;
            canvas.width = canvasWidth;
            canvas.height = canvasHeight;

            ctx.clearRect(0, 0, canvasWidth, canvasHeight);

            const borderWidth = 5;
            const radius = 60;
            const innerWidth = canvasWidth - (borderWidth * 2);
            const innerHeight = canvasHeight - borderWidth;

            // Calcular altura del líquido según el tipo de sesión
            let liquidPercentage;
            const sessionType = sessionPlan[currentSessionIndex] || 'TRABAJO';
            
            if (sessionType === 'TRABAJO') {
                liquidPercentage = (remainingSeconds / totalSeconds) * 100;
            } else {
                liquidPercentage = ((totalSeconds - remainingSeconds) / totalSeconds) * 100;
            }
            
            const liquidHeight = (innerHeight * liquidPercentage) / 100;
            const liquidY = canvasHeight - liquidHeight;

            ctx.save();

            // Forma de la probeta
            ctx.beginPath();
            ctx.moveTo(borderWidth, 0);
            ctx.lineTo(borderWidth, canvasHeight - radius - borderWidth);
            ctx.arcTo(borderWidth, canvasHeight - borderWidth, borderWidth + radius, canvasHeight - borderWidth, radius);
            ctx.lineTo(canvasWidth - radius - borderWidth, canvasHeight - borderWidth);
            ctx.arcTo(canvasWidth - borderWidth, canvasHeight - borderWidth, canvasWidth - borderWidth, canvasHeight - radius - borderWidth, radius);
            ctx.lineTo(canvasWidth - borderWidth, 0);
            ctx.closePath();
            ctx.clip();

            // Dibujar líquido
            const currentLiquidColor = liquid.style.backgroundColor || '#3498db';
            ctx.fillStyle = currentLiquidColor;
            ctx.fillRect(borderWidth, liquidY, innerWidth, liquidHeight);

            // Dibujar burbujas solo en descansos
            if (isRunning && (sessionType === 'CORTO' || sessionType === 'LARGO') && liquidHeight > 20) {
                if (Math.random() < 0.05) {
                    createPipBubble();
                }

                for (let i = pipBubbles.length - 1; i >= 0; i--) {
                    const bubble = pipBubbles[i];
                    
                    bubble.y -= bubble.speed;
                    bubble.life += 0.03;
                    bubble.x += Math.sin(bubble.life * 4 + bubble.wobble) * 0.5;
                    bubble.opacity = Math.max(0, 1 - (bubble.life / 3));
                    
                    if (bubble.y < liquidY - 10 || bubble.opacity <= 0 || bubble.life > 3) {
                        pipBubbles.splice(i, 1);
                        continue;
                    }
                    
                    ctx.globalAlpha = bubble.opacity * 0.8;
                    
                    let bubbleHue = currentLiquidHue || 200;
                    const match = currentLiquidColor.match(/hsl\((\d+)/);
                    if (match) {
                        bubbleHue = parseInt(match[1]);
                    }
                    
                    const contrastHue = (bubbleHue + 180) % 360;
                    ctx.fillStyle = `hsl(${contrastHue}, 90%, 75%)`;
                    
                    ctx.beginPath();
                    ctx.arc(bubble.x, bubble.y, bubble.size / 2, 0, Math.PI * 2);
                    ctx.fill();
                    
                    // Highlight en la burbuja
                    ctx.fillStyle = '#ffffff';
                    ctx.beginPath();
                    ctx.arc(bubble.x - bubble.size * 0.1, bubble.y - bubble.size * 0.1, bubble.size * 0.2, 0, Math.PI * 2);
                    ctx.fill();
                    
                    ctx.globalAlpha = 1;
                }
            }

            ctx.restore();

            // Borde de la probeta
            ctx.strokeStyle = '#ccc';
            ctx.lineWidth = borderWidth;
            ctx.lineCap = 'round';
            ctx.lineJoin = 'round';

            ctx.beginPath();
            ctx.moveTo(borderWidth / 2, 0);
            ctx.lineTo(borderWidth / 2, canvasHeight - radius - borderWidth / 2);
            ctx.arcTo(borderWidth / 2, canvasHeight - borderWidth / 2, borderWidth / 2 + radius, canvasHeight - borderWidth / 2, radius);
            ctx.lineTo(canvasWidth - radius - borderWidth / 2, canvasHeight - borderWidth / 2);
            ctx.arcTo(canvasWidth - borderWidth / 2, canvasHeight - borderWidth / 2, canvasWidth - borderWidth / 2, canvasHeight - radius - borderWidth / 2, radius);
            ctx.lineTo(canvasWidth - borderWidth / 2, 0);
            ctx.stroke();

            // Texto del timer
            const fontSize = Math.floor(canvasWidth * 0.15);
            ctx.font = `bold ${fontSize}px sans-serif`;
            ctx.fillStyle = '#fff';
            ctx.textAlign = 'center';
            ctx.textBaseline = 'middle';
            ctx.fillText(timerLabel.textContent, canvasWidth / 2, canvasHeight * 0.15);
            
            // Texto del tipo de sesión
            const smallFontSize = Math.floor(canvasWidth * 0.08);
            ctx.font = `${smallFontSize}px sans-serif`;
            ctx.fillStyle = '#f7b733';
            const sessionTypeText = sessionType === 'TRABAJO' ? 'Trabajo' : 
                                  sessionType === 'CORTO' ? 'Descanso' : 'Descanso Largo';
            ctx.fillText(sessionTypeText, canvasWidth / 2, canvasHeight * 0.25);
        };
        
        drawFrame();

        video.srcObject = canvas.captureStream(30);
        await video.play();
        await video.requestPictureInPicture();

        const animationLoop = () => {
            if (!document.pictureInPictureElement) {
                pipBubbles = [];
                video.remove();
                canvas.remove();
                return;
            }
            drawFrame();
            requestAnimationFrame(animationLoop);
        };
        requestAnimationFrame(animationLoop);

        video.addEventListener('leavepictureinpicture', () => {
            pipBubbles = [];
            video.remove();
            canvas.remove();
        });

    } catch (error) {
        console.error("Falló la activación de PiP:", error);
        alert("Ocurrió un error al intentar activar la mini pantalla.");
    }
}

// === EVENT LISTENERS ===
document.addEventListener('DOMContentLoaded', function() {
    // Agregar los estilos adicionales
    agregarEstilosAdicionales();
    
    console.log('DOM cargado, configurando event listeners...');
    
    // Manejo de errores global para móviles
    window.addEventListener('error', function(e) {
        console.error('Error global capturado:', e.error);
        console.error('En archivo:', e.filename, 'línea:', e.lineno);
    });
    
    window.addEventListener('unhandledrejection', function(e) {
        console.error('Promise rechazada no manejada:', e.reason);
    });
    
    // Event listeners principales
    if (setTimeBtn) {
        setTimeBtn.addEventListener('click', function(e) {
            e.preventDefault();
            console.log('Botón empezar sesión clickeado');
            setupTimer();
        });
        
        // Añadir también el evento 'touchstart' para móviles
        setTimeBtn.addEventListener('touchstart', function(e) {
            e.preventDefault();
            console.log('Touch start en botón empezar sesión');
            setupTimer();
        });
    } else {
        console.error('setTimeBtn no encontrado!');
    }
    
    if (newSessionBtn) {
        newSessionBtn.addEventListener('click', function(e) {
            e.preventDefault();
            console.log('Botón nueva sesión clickeado');
            resetTimer();
        });
    }
    
    if (resetBtn) {
        resetBtn.addEventListener('click', function(e) {
            e.preventDefault();
            console.log('Botón reset clickeado');
            resetTimer();
        });
    }
    
    if (skipBtn) {
        skipBtn.addEventListener('click', function(e) {
            e.preventDefault();
            console.log('Botón saltar clickeado');
            skipSession();
        });
    }

    if (pauseBtn) {
        pauseBtn.addEventListener('click', function(e) {
            e.preventDefault();
            console.log('Botón pausa clickeado, isRunning:', isRunning, 'isPaused:', isPaused);
            if (isRunning) {
                pauseTimer();
            } else if (isPaused) {
                startTimer();
            }
        });
    }
    
    // Event listener para Picture-in-Picture
    if (pipBtn) {
        pipBtn.addEventListener('click', function(e) {
            e.preventDefault();
            console.log('Botón PiP clickeado');
            activarPictureInPicture();
        });
    }
    
    console.log('Todos los event listeners configurados');
    
    // Test de elementos del DOM
    console.log('Elementos encontrados:');
    console.log('- setTimeBtn:', !!setTimeBtn);
    console.log('- configPanel:', !!configPanel);
    console.log('- timerDisplay:', !!timerDisplay);
    console.log('- totalHoursInput:', !!totalHoursInput);
    console.log('- workMinutesInput:', !!workMinutesInput);
});