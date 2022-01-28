/**
 * Created by Make on 02.10.2017.
 */



function RectangleTool(canvas, context, image) {
    var object = {
        canvas: null,
        context: null,
        isOnline: false,
        isDrawingState: false,
        x: null,
        y: null,
        image: null,
        lastCoorinates: null,
        drawingStateChanged: function (state) {
        },
        onRectangleDrawed: function (x, y, h, w) {
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
                x: _x, // 4 is padding thimbnail class
                y: _y // 4 is padding thimbnail class
            };
        },
        startDrawRectangle: function (e) {
            var $this = object;
            if ($this.isOnline || !$this.isInited()) {
                return;
            }
            if (e.which == 1) { //only left btn

                if ($this.isDrawingState) {
                    $this.resetDrawingQuad();
                }

                var coords = $this.getMousePos(e);
                $this.x = coords.x;
                $this.y = coords.y;
                $this.canvas.addEventListener('mousemove', $this.mouseMove);
                $this.isDrawingState = true;
                $this.emitStateChanged();
                $this.canvas.removeEventListener('mousedown', $this.startDrawRectangle);
                $this.canvas.addEventListener('mouseup', $this.stopDrawRectangle);
            }
        },
        mouseMove: function (e) {
            var $this = object;
            var coords = $this.getMousePos(e);
            $this.clearCanvas();
            context.strokeStyle = "#FCFF00";
            context.lineWidth = 4;
            var x, y, width, height;
            [x, y, width, height] = $this.resolveRectCoords(coords);
            $this.context.strokeRect(x, y, width, height);
            $this.onRectangleDrawed(x, y, width, height, null);
            $this.lastCoorinates = [x, y, width, height];
        },
        resolveRectCoords: function (coords) {
            var $this = object;
            var x, width, y, height;
            if (coords.x > $this.x) {
                x = $this.x;
                width = coords.x - $this.x;
            } else {
                x = coords.x;
                width = $this.x - coords.x;
            }

            if (coords.y > $this.y) {
                y = $this.y;
                height = coords.y - $this.y;
            } else {
                y = coords.y;
                height = $this.y - coords.y;
            }
            return [x, y, width, height];
        },
        stopDrawRectangle: function (e) {
            var $this = object;
            $this.canvas.removeEventListener('mousemove', $this.mouseMove);
            var coords = $this.getMousePos(e);
            $this.mouseMove(e);// lastRender
            var x, y, width, height;
            [x, y, width, height] = $this.resolveRectCoords(coords);
            var image = $this.resolveImage64(x, y, width, height);
            $this.onRectangleDrawed(x, y, width, height, image);
            $this.isDrawingState = false;
            $this.emitStateChanged();
            $this.lastCoorinates = [x, y, width, height];
            $this.canvas.removeEventListener('mouseup', $this.stopDrawRectangle);
            $this.canvas.addEventListener('mousedown', $this.startDrawRectangle);
        },
        init: function (canvas, context, image) {
            var $this = object;
            $this.canvas = canvas;
            $this.context = context;
            $this.image = image;
            $this.canvas.addEventListener('mousedown', $this.startDrawRectangle);
        },
        isInited: function () {
            var $this = object;
            return $this.canvas && $this.context && $this.image;
        },
        release: function () {
            var $this = object;
            $this.canvas.removeEventListener('mousemove', $this.mouseMove);
            $this.canvas.removeEventListener('mouseup', $this.stopDrawRectangle);
            $this.canvas.removeEventListener('mousedown', $this.startDrawRectangle);
            $this.lastCoorinates = null;
            $this.clearCanvas();
            $this.image = null;
            $this.context = null;
            $this.canvas = null;
        },
        clearCanvas: function () {
            var $this = object;
            $this.context.drawImage($this.image, 0, 0, $this.image.width, $this.image.height);
        },
        resetDrawingQuad: function () {
            var $this = object;
            $this.clearCanvas();
            $this.x = null;
            $this.y = null;
        },
        update: function (image, lastCoords) {
            var $this = object;
            $this.image = image;
            if (lastCoords) {
                $this.lastCoorinates = lastCoords;
            }
            if ($this.lastCoorinates) {
                var x, y, width, height;
                [x, y, width, height] = $this.lastCoorinates;
                context.strokeStyle = "#FCFF00";
                context.lineWidth = 4;
                $this.clearCanvas();
                $this.context.strokeRect(x, y, width, height);
            } else {
                $this.clearCanvas();
            }
        },
        setAvailableCoords: function (lastCoords) {
            var $this = object;
            $this.lastCoorinates = lastCoords;
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
        resolveImage64: function (x, y, width, height) {
            var $this = object;
            $this.clearCanvas();
            var image64 = $this.canvas.toDataURL("image/jpeg");
            $this.context.strokeRect(x, y, width, height);
            return image64;
        }
    };
    object.init(canvas, context, image);
    return object;
}