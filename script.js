// --- Elementos del DOM ---
const configPanel = document.getElementById('config-panel');
const timerDisplay = document.getElementById('timer-display');
const setTimeBtn = document.getElementById('set-time-btn');
const sessionMinutesInput = document.getElementById('session-minutes');
const timerLabel = document.getElementById('timer-label');
const beaker = document.getElementById('beaker-container');
const liquid = document.getElementById('liquid');
const pauseBtn = document.getElementById('pause-btn');
const resetBtn = document.getElementById('reset-btn');
const pipBtn = document.getElementById('pip-btn');
const pauseBtnText = document.querySelector('#pause-btn span');

// --- Variables del Temporizador ---
let timerInterval;
let totalSeconds;
let remainingSeconds;
let isRunning = false;
let currentLiquidHue;

// --- Variables para burbujas en PiP ---
let pipBubbles = [];

// --- Variables para Wake Lock API ---
let wakeLock = null;

// --- Funciones Principales (con Wake Lock) ---
async function setupTimer() {
    const minutes = parseInt(sessionMinutesInput.value, 10);
    if (isNaN(minutes) || minutes < 1) {
        alert("Por favor, ingresa un número válido de minutos.");
        return;
    }
    totalSeconds = minutes * 60;
    remainingSeconds = totalSeconds;
    configPanel.classList.add('hidden');
    timerDisplay.classList.remove('hidden');
    setRandomLiquidColor();
    updateUI();
    
    // Activamos el Wake Lock para mantener la pantalla encendida
    await requestWakeLock();
    
    startTimer();
}

async function startTimer() {
    if (isRunning) return;
    isRunning = true;
    pauseBtnText.textContent = "Pausar";
    
    // Reactivamos Wake Lock si no está activo
    if (!wakeLock) {
        await requestWakeLock();
    }
    
    timerInterval = setInterval(() => {
        remainingSeconds--;
        updateUI();
        if (isRunning) createBubble();
        if (remainingSeconds <= 0) {
            clearInterval(timerInterval);
            isRunning = false;
            releaseWakeLock(); // Liberamos el Wake Lock al terminar
            alert("¡Tiempo completado!");
            pauseBtnText.textContent = "Continuar";
            
            // Opcional: reproducir sonido o vibración
            if ('vibrate' in navigator) {
                navigator.vibrate([500, 200, 500, 200, 500]);
            }
        }
    }, 1000);
}

function pauseTimer() {
    isRunning = false;
    clearInterval(timerInterval);
    releaseWakeLock(); // Liberamos Wake Lock al pausar
    pauseBtnText.textContent = "Continuar";
}

function resetTimer() {
    pauseTimer();
    releaseWakeLock(); // Liberamos Wake Lock al resetear
    configPanel.classList.remove('hidden');
    timerDisplay.classList.add('hidden');
}

// --- Funciones Wake Lock API ---
async function requestWakeLock() {
    if ('wakeLock' in navigator) {
        try {
            wakeLock = await navigator.wakeLock.request('screen');
            console.log('Wake Lock activado - la pantalla se mantendrá encendida');
            
            // Reactivar automáticamente si se pierde (ej: cambio de pestaña)
            wakeLock.addEventListener('release', () => {
                console.log('Wake Lock liberado');
                if (isRunning) {
                    // Reintentamos activar Wake Lock si el timer sigue corriendo
                    setTimeout(requestWakeLock, 1000);
                }
            });
            
        } catch (err) {
            console.log('No se pudo activar Wake Lock:', err.message);
            // Fallback: mostrar mensaje al usuario
            if (isRunning) {
                console.log('Tip: Mantén esta pestaña activa para que el timer funcione correctamente');
            }
        }
    } else {
        console.log('Wake Lock API no soportada en este navegador');
    }
}

function releaseWakeLock() {
    if (wakeLock) {
        wakeLock.release();
        wakeLock = null;
        console.log('Wake Lock liberado manualmente');
    }
}

// Reactivar Wake Lock cuando la página vuelve a estar visible
document.addEventListener('visibilitychange', async () => {
    if (!document.hidden && isRunning && !wakeLock) {
        await requestWakeLock();
    }
});

// --- Funciones de UI y Efectos (sin cambios) ---
function updateUI() {
    const percentage = (remainingSeconds / totalSeconds) * 100;
    liquid.style.height = `${percentage}%`;
    const mins = Math.floor(remainingSeconds / 60);
    const secs = remainingSeconds % 60;
    timerLabel.textContent = `${String(mins).padStart(2, '0')}:${String(secs).padStart(2, '0')}`;
}

function setRandomLiquidColor() {
    currentLiquidHue = Math.floor(Math.random() * 360);
    const newColor = `hsl(${currentLiquidHue}, 80%, 60%)`;
    liquid.style.backgroundColor = newColor;
}

function createBubble() {
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
    beaker.appendChild(bubble);
    setTimeout(() => bubble.remove(), 5000);
}

// --- Asignación de Eventos (sin cambios) ---
setTimeBtn.addEventListener('click', setupTimer);
resetBtn.addEventListener('click', resetTimer);
pauseBtn.addEventListener('click', () => {
    if (isRunning) {
        pauseTimer();
    } else {
        if (remainingSeconds > 0) startTimer();
    }
});

// --- Lógica Picture-in-Picture (BURBUJAS CORREGIDAS) ---
pipBtn.addEventListener('click', async () => {
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

        // --- FUNCIÓN PARA CREAR BURBUJAS EN PiP ---
        const createPipBubble = () => {
            if (!isRunning) return;
            
            const bubble = {
                x: Math.random() * (video.width - 40) + 20,
                y: video.height - 20,
                size: Math.random() * 8 + 4, // Más grandes para ser más visibles
                speed: Math.random() * 2 + 1, // Más rápidas
                opacity: 1,
                life: 0,
                wobble: Math.random() * Math.PI * 2 // Para movimiento diferente
            };
            pipBubbles.push(bubble);
            console.log('Burbuja creada en PiP:', pipBubbles.length); // Debug
        };

        // --- FUNCIÓN DE DIBUJADO MEJORADA ---
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

            // Calculamos altura del líquido
            const liquidPercentage = (remainingSeconds / totalSeconds) * 100;
            const liquidHeight = (innerHeight * liquidPercentage) / 100;
            const liquidY = canvasHeight - liquidHeight;

            // Guardamos estado para clipping
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

            // --- DIBUJAR BURBUJAS (VERSIÓN CORREGIDA) ---
            if (isRunning && liquidHeight > 20) {
                // Crear nueva burbuja ocasionalmente
                if (Math.random() < 0.05) { // 5% de probabilidad cada frame
                    createPipBubble();
                }

                // Actualizar y dibujar burbujas existentes
                for (let i = pipBubbles.length - 1; i >= 0; i--) {
                    const bubble = pipBubbles[i];
                    
                    // Actualizar posición
                    bubble.y -= bubble.speed;
                    bubble.life += 0.03;
                    bubble.x += Math.sin(bubble.life * 4 + bubble.wobble) * 0.5;
                    bubble.opacity = Math.max(0, 1 - (bubble.life / 3));
                    
                    // Remover burbujas viejas o fuera del líquido
                    if (bubble.y < liquidY - 10 || bubble.opacity <= 0 || bubble.life > 3) {
                        pipBubbles.splice(i, 1);
                        continue;
                    }
                    
                    // Dibujar burbuja
                    ctx.globalAlpha = bubble.opacity * 0.8;
                    
                    // Color contrastante más visible
                    let bubbleHue = currentLiquidHue || 200; // Fallback si no hay hue
                    if (typeof currentLiquidHue === 'undefined') {
                        // Extraer hue del color actual si es posible
                        const match = currentLiquidColor.match(/hsl\((\d+)/);
                        if (match) {
                            bubbleHue = parseInt(match[1]);
                        }
                    }
                    
                    const contrastHue = (bubbleHue + 180) % 360;
                    ctx.fillStyle = `hsl(${contrastHue}, 90%, 75%)`;
                    
                    // Dibujar con efecto de brillo
                    ctx.beginPath();
                    ctx.arc(bubble.x, bubble.y, bubble.size / 2, 0, Math.PI * 2);
                    ctx.fill();
                    
                    // Añadir brillo interno
                    ctx.globalAlpha = bubble.opacity * 0.4;
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
            const fontSize = Math.floor(canvasWidth * 0.2);
            ctx.font = `bold ${fontSize}px sans-serif`;
            ctx.fillStyle = '#fff';
            ctx.textAlign = 'center';
            ctx.textBaseline = 'middle';
            ctx.fillText(timerLabel.textContent, canvasWidth / 2, canvasHeight * 0.15);
        };
        
        drawFrame();

        video.srcObject = canvas.captureStream(30);
        await video.play();
        await video.requestPictureInPicture();

        // Loop de animación más frecuente
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
});