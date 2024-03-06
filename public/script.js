const canvas = document.querySelector('.myCanvas');
const toolbar = document.querySelector('.toolbar');
const ctx = canvas.getContext('2d');

const btnRewind = document.querySelector('button[id="btnRewind"]');
const btnStepBack = document.querySelector('button[id="btnStepBack"]');
const btnPlay = document.querySelector('button[id="btnPlay"]');
const btnStepForward = document.querySelector('button[id="btnStepForward"]');

let scale = 1.0;
let width = canvas.width;
let height = canvas.height; 
calculateScale();

ctx.fillStyle = 'rgb(0,0,64)';
ctx.fillRect(0,0,width,height);

let backgroundCanvas = null;
let queryCanvas = null;
let imageMap = new Map();
let animation = null;
let currentStep = 0;
let currentAction = 0;
let progress = 0.0;
let expectedImages = 0;
let loadedImages = 0;
let playMode = false;
let animationRunning = false;


window.onresize = calculateScale;

fetch('/animation')
	.then(response => response.json())
	.then(myJson => {
		animation = myJson;
		loadImages();
	});
queryCanvas = new Image();
queryCanvas.src = '/query';
queryCanvas.onload = drawBackground;

btnStepForward.addEventListener('click', () => {
	playMode = false;
	animationRunning = true;
	draw();		
});

btnPlay.addEventListener('click', () => {
	playMode = true;
	animationRunning = true;
	if (expectedImages==loadedImages) draw();		
});

btnStepBack.addEventListener('click', () => {
	playMode = false;
	if (currentAction == 0) {
		currentStep--;
	} else {
		currentAction = 0
	}
	if (currentStep < 0) currentStep = 0;
	loadImages();
});

btnRewind.addEventListener('click', () => {
	playMode = false;
	currentStep = 0;
	currentAction = 0;
	loadImages();
	drawBackground();
	});


function calculateScale(){
	canvas.width = window.innerWidth;
	canvas.height = window.innerHeight - 25;
	width = canvas.width;
	height = canvas.height; 
	scale = (width / 1280) < (height / 720) ? (width / 1280) : (height / 720); 
}

function nextAction(){
	if (currentAction < animation.steps[currentStep].actions.length-1) {
		currentAction++;
		loadImages();
	}
	else {
		currentAction = 0;
		if (currentStep < animation.steps.length-1){
			currentStep++;
			loadImages();
		}
	}
}


function loadImages(){
	animationRunning = false;
	progress = 0.0;
	expectedImages = 0;
	loadedImages = 0;
	imageMap = new Map();
	backgroundCanvas = null;
	if (animation.steps[currentStep].actions[currentAction].prevCanvas != null)
	{
		backgroundCanvas = new Image();
		backgroundCanvas.src = animation.steps[currentStep].actions[currentAction].prevCanvas;
	}
	for (transition of animation.steps[currentStep].actions[currentAction].transitions){
		for (cellPosition of transition.cellPositions){
			image = new Image();
			image.src = '/cell?id=' + cellPosition.id;
			expectedImages++;
			imageMap.set(cellPosition.id, image)
			image.onload = () => {
				loadedImages++;
				if (playMode && expectedImages==loadedImages){
					animationRunning = true;
					draw();
				}		
			};
		}
	}
}

function drawBackground(){
	ctx.fillStyle = 'rgb(255,255,255)';
	ctx.fillRect(0,0,width,height);
	ctx.drawImage(queryCanvas, 0, 0, scale * queryCanvas.width, scale * queryCanvas.height);
}

function draw() {
	if (animationRunning){
		drawBackground();
		progress += 0.01;
		
		if (backgroundCanvas != null){
			ctx.drawImage(backgroundCanvas, scale *  animation.querywidth, 0, scale * backgroundCanvas.width, scale * backgroundCanvas.height);
		}
		for (transition of animation.steps[currentStep].actions[currentAction].transitions){
			if (transition.type == 'Move'){
				let p = (1.0 - Math.cos(progress * Math.PI)) / 2;
				image = p < 0.5 ? imageMap.get(transition.cellPositions[0].id) : imageMap.get(transition.cellPositions[1].id);
				dx = transition.cellPositions[0].x + (transition.cellPositions[1].x - transition.cellPositions[0].x) * p;
				dy = transition.cellPositions[0].y + (transition.cellPositions[1].y - transition.cellPositions[0].y) * p;
				dWidth = transition.cellPositions[0].w + (transition.cellPositions[1].w - transition.cellPositions[0].w) * p;
				dHeight = transition.cellPositions[0].h + (transition.cellPositions[1].h - transition.cellPositions[0].h) * p;
				ctx.drawImage(image, scale * dx, scale * dy, scale * dWidth, scale * dHeight)
			} else if (transition.type == 'Spawning'){
				image = imageMap.get(transition.cellPositions[0].id);
				dx = transition.cellPositions[0].x + transition.cellPositions[0].w * (1-progress) / 2;
				dy = transition.cellPositions[0].y + transition.cellPositions[0].h * (1-progress) / 2;
				dWidth = transition.cellPositions[0].w  * progress;
				dHeight = transition.cellPositions[0].h * progress;
				ctx.drawImage(image, scale * dx, scale * dy, scale * dWidth, scale * dHeight)
			} else if (transition.type == 'Vanishing'){
				image = imageMap.get(transition.cellPositions[0].id);
				dx = transition.cellPositions[0].x + transition.cellPositions[0].w * progress / 2;
				dy = transition.cellPositions[0].y + transition.cellPositions[0].h * progress / 2;
				dWidth = transition.cellPositions[0].w  * (1-progress);
				dHeight = transition.cellPositions[0].h * (1-progress);
				ctx.drawImage(image, scale * dx, scale * dy, scale * dWidth, scale * dHeight)
			}
		}
		if (progress < 1.0) {
			window.requestAnimationFrame(draw);
		} else {
			ctx.fillStyle = 'rgb(200,0,0)';
			ctx.fillRect(0,0,10,10);
			nextAction();
		}
	}
};
