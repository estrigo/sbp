/**
 * Created by Make on 02.10.2017.
 */



function PolygonTool(canvas, context, image) {
    var object = {
        canvas: null,
        context: null,
        isDrawingState: false,
        isOnline: false,
        points: [],
        movePoint: null,
        image: null,
        canvasDirty: false,
        frameFps: 100,
        fps: 1000,
        renderColor: "#00FFFA",
        activeColor: "#3EFF00",
        defaultColor: "#00FFFA",
        errorColor: "#FF0003",
        drawingStateChanged: function (state) {
        },
        onPolygonDrawed: function () {
        },
        getMousePos: function (evt) {
            var $this = object;
            var rect = $this.canvas.getBoundingClientRect();
            var canvasWidth = rect.width;
            var canvasHeight = rect.height;

            var x = (evt.clientX - (rect.left));
            var _x = (($this.image.width) * x) / canvasWidth;
            var y = evt.clientY - (rect.top);
            var _y = (($this.image.height) * y) / canvasHeight;

            return {
                x: Math.round(_x), // 4 is padding thimbnail class
                y: Math.round(_y) // 4 is padding thimbnail class
            };
        },
        mouseMove: function (e) {
            var $this = object;
            var coords = $this.getMousePos(e);
            $this.movePoint = coords;

            if ($this.points.length > 1) {
                if ($this.points.length >= 3 && ($this.distanceBetweenFirst(coords) <= 20)) {
                    $this.renderColor = $this.activeColor;
                } else {
                    if ($this.points.length < 3 && ($this.distanceBetweenFirst(coords) <= 20)) {
                        $this.renderColor = $this.errorColor;
                    } else {
                        $this.renderColor = $this.defaultColor;
                    }
                }
            } else {
                $this.renderColor = $this.defaultColor;
            }
        },
        mouseClick: function (e) {
            var $this = object;
            if ($this.isOnline) {
                return;
            }
            var coords = $this.getMousePos(e);
            if ($this.points.length > 1) {
                if ($this.isDrawingState) {
                    if ($this.points.length >= 3 && ($this.distanceBetweenFirst(coords) <= 20)) {
                        $this.points.push({x: $this.points[0].x, y: $this.points[0].y});
                        $this.stopUpdateFrame();
                        $this.clearCanvas();
                        var image = $this.resolveImage64();
                        $this.renderGeometry();
                        $this.onPolygonDrawed($this.points.slice(), image);
                        return;
                    }
                    if ($this.points.length < 3 && ($this.distanceBetweenFirst(coords) <= 20)) {
                        return;
                    }
                } else {
                    $this.points = [];//reset all
                    $this.onPolygonDrawed([], null);
                }
            }

            $this.points.push(coords);
            $this.startUpdateFrame(); //always frame updater
        },
        distanceBetweenFirst: function (point) {
            var $this = object;
            var point2 = $this.points[0];
            var a = point.x - point2.x;
            var b = point.y - point2.y;
            var c = Math.sqrt( a*a + b*b );
            return Math.round(c);
        },
        rightClick: function (e) {
            e.preventDefault();
            var $this = object;
            $this.points = [];
            $this.onPolygonDrawed([], null);
            $this.canvasDirty = true;
            $this.clearCanvas();
            $this.isDrawingState = false;
            $this.emitStateChanged();
        },
        init: function (canvas, context, image) {
            var $this = object;
            $this.canvas = canvas;
            $this.context = context;
            $this.image = image;
            $this.canvas.addEventListener('click', $this.mouseClick);
            $this.canvas.addEventListener('contextmenu', $this.rightClick);
            $this.canvas.addEventListener('mousemove', $this.mouseMove);
            $this.fps = Math.round(1000 / $this.frameFps);
        },
        isInited: function () {
            var $this = object;
            return $this.canvas && $this.context && $this.image;
        },
        release: function () {
            var $this = object;
            $this.canvas.removeEventListener('mousemove', $this.mouseMove);
            $this.canvas.removeEventListener('click', $this.mouseClick);
            $this.canvas.removeEventListener('contextmenu', $this.rightClick);
            //clear them self
            $this.canvasDirty = true;
            $this.clearCanvas();
            $this.context = null;
            $this.canvas = null;
            $this.image = null;
        },
        clearCanvas: function () {
            var $this = object;
            if ($this.canvasDirty) {
                $this.context.drawImage($this.image, 0, 0, $this.image.width, $this.image.height);
                $this.canvasDirty = false;
            }
        },
        update: function (image, points) {
            var $this = object;
            $this.image = image;
            if (!$this.isDrawingState) {
                $this.canvasDirty = true;
                $this.clearCanvas();
                $this.movePoint = null;
                $this.renderColor = $this.activeColor;
                if (points) {
                    $this.points = points;
                }
                $this.renderGeometry();
            }
        },
        startUpdateFrame: function () {
            var $this = object;
            $this.isDrawingState = true;
            $this.emitStateChanged();
            setTimeout($this.updateFrame, $this.fps);
        },
        stopUpdateFrame: function () {
            var $this = object;
            $this.isDrawingState = false;
            $this.emitStateChanged();
        },
        updateFrame: function () {
            var $this = object;
            if ($this.isDrawingState) {
                $this.clearCanvas();
                $this.renderGeometry();
                setTimeout($this.updateFrame, $this.fps);
            }
        },
        renderGeometry: function () {
            var $this = object;
            var move = $this.movePoint ? 1 : 0;
            if (($this.points.length + move) > 1) {

                $this.context.strokeStyle = $this.renderColor;
                $this.context.lineWidth = 3;
                $this.context.beginPath();

                for (var i = 0; i < $this.points.length; i++) {
                    var point = $this.points[i];
                    if (i == 0) {
                        $this.context.moveTo(point.x, point.y);
                    } else {
                        $this.context.lineTo(point.x, point.y);
                    }
                }

                if ($this.movePoint) {
                    $this.context.lineTo($this.movePoint.x, $this.movePoint.y);
                }

                $this.context.closePath();
                $this.context.stroke();

                $this.canvasDirty = true;
            }
        },
        emitStateChanged: function () {
            var $this = object;
            $this.drawingStateChanged($this.isDrawingState);
        },
        onlineUpdateEnabled: function () {
            var $this = object;
            $this.isOnline = true;
        },
        onlineUpdateDisabled: function () {
            var $this = object;
            $this.isOnline = false;
        },
        resolveImage64: function () {
            var $this = object;
            return $this.canvas.toDataURL("image/jpeg");
        },
        setAvailableCoords: function (points) {
            var $this = object;
            $this.points = points;
        }
    };

    object.init(canvas, context, image);
    return object;
}