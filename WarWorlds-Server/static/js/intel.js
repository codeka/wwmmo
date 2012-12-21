/* Simple JavaScript Inheritance
 * By John Resig http://ejohn.org/
 * MIT Licensed.
 */
// Inspired by base2 and Prototype
(function(){
  var initializing = false, fnTest = /xyz/.test(function(){xyz;}) ? /\b_super\b/ : /.*/;
  // The base Class implementation (does nothing)
  this.Class = function(){};

  // Create a new Class that inherits from this class
  Class.extend = function(prop) {
    var _super = this.prototype;

    // Instantiate a base class (but only create the instance,
    // don't run the init constructor)
    initializing = true;
    var prototype = new this();
    initializing = false;

    // Copy the properties over onto the new prototype
    for (var name in prop) {
      // Check if we're overwriting an existing function
      prototype[name] = typeof prop[name] == "function" && 
        typeof _super[name] == "function" && fnTest.test(prop[name]) ?
        (function(name, fn){
          return function() {
            var tmp = this._super;
            
            // Add a new ._super() method that is the same method
            // but on the super-class
            this._super = _super[name];

            // The method only need to be bound temporarily, so we
            // remove it when we're done executing
            var ret = fn.apply(this, arguments);        
            this._super = tmp;

            return ret;
          };
        })(name, prop[name]) :
        prop[name];
    }

    // The dummy class constructor
    function Class() {
      // All construction is actually done in the init method
      if ( !initializing && this.init )
        this.init.apply(this, arguments);
    }

    // Populate our constructed prototype object
    Class.prototype = prototype;

    // Enforce the constructor to be what we expect
    Class.prototype.constructor = Class;

    // And make this class extendable
    Class.extend = arguments.callee;

    return Class;
  };
})();

$(function() {

  //
  // Helper class that encapsulates functionality of a 2D vector.
  //
  var Vector2 = Class.extend({
    init: function(x, y) {
      if (typeof y == "undefined") {
        this.x = x.x;
        this.y = x.y;
      } else {
        this.x = x;
        this.y = y;
      }
    },

    length: function() {
      return Math.sqrt((this.x * this.x) + (this.y * this.y));
    },

    normalize: function() {
      var scale = 1.0 / this.length();
      this.x *= scale;
      this.y *= scale;
    },

    add: function(pt) {
      this.x += pt.x;
      this.y += pt.y;
    },

    directionTo: function(pt) {
      var v = new Vector2(pt.x - this.x, pt.y - this.y);
      v.normalize();
      return v;
    },

    scale: function(s) {
      this.x *= s;
      this.y *= s;
    }
  });


  //
  // Base class for any object that appears in the "world". Has a position,
  // size and image. The size is used to render the image (which we assume
  // to be a square).
  //
  var WorldObject = Class.extend({
    init: function(position, img, size) {
      this.world = null; // the world will set this up for us
    },

    update: function(now) {},

    render: function(context) {}
  });

  var Sprite = WorldObject.extend({
    init: function(position, img, size) {
      this.position = position;
      this.img = img;
      this.size = size;
    },

    render: function(context) {
      if (!this.img.complete) {
        return;
      }

      if (this.size == 0) {
        context.drawImage(this.img,
                          this.position.x,
                          this.position.y);
      } else {
        context.drawImage(this.img,
                          this.position.x - (this.size/2),
                          this.position.y - (this.size/2),
                          this.size, this.size);
      }
    }
  });

  var Star = WorldObject.extend({
    init: function(pb) {
      this.name = pb.name;
      this.type = pb.classification;
      this.sectorX = pb.sector_x;
      this.sectorY = pb.sector_y;
      this.offsetX = pb.offset_x;
      this.offsetY = pb.offset_y;
      this.key = pb.key;
      this.size = pb.size;
    },

    render: function(context, offsetX, offsetY) {
      context.fillRect(offsetX + this.offsetX - (this.size / 2),
                       offsetY + this.offsetY - (this.size / 2),
                       this.size, this.size);

      context.textAlign = "center";
      context.font = "12pt sans-serif"
      context.fillText(this.name,
                      offsetX + this.offsetX,
                      offsetY + this.offsetY + this.size + 6);
    }
  });

  var Sector = WorldObject.extend({
    init: function(pb) {
      this.sectorX = pb.x;
      this.sectorY = pb.y;
      this.stars = [];
      for (var i = 0; i < pb.stars.length; i++) {
        var star = new Star(pb.stars[i]);
        this.stars.push(star);
      }
    },

    render: function(context) {
      var x = this.world.offsetX + Sector.SIZE * this.sectorX;
      var y = this.world.offsetY + Sector.SIZE * this.sectorY;
      context.strokeRect(x, y, Sector.SIZE, Sector.SIZE);

      for (var i = 0; i < this.stars.length; i++) {
        this.stars[i].render(context, x, y);
      }
    }
  });
  Sector.SIZE = 1024;

  //
  // The World is the container for all the objects.
  //
  var World = Class.extend({
    init: function() {
      this._objects = [];
      this.offsetX = 0;
      this.offsetY = 0;
    },

    drag: function(dx, dy) {
      this.offsetX += dx;
      this.offsetY += dy;
      this.draw();
    },

    addObject: function(obj) {
      obj.world = this;
      this._objects.push(obj);
    },

    update: function(now) {
      for (var i = 0; i < this._objects.length; i++) {
        this._objects[i].update(now);
      }
    },

    render: function(canvas) {
      var context = canvas.getContext("2d");
      context.fillRect(0, 0, canvas.width, canvas.height);
      context.fillStyle = "#fff";
      context.strokeStyle = "#fff";

      for (var i = 0; i < this._objects.length; i++) {
        this._objects[i].render(context);
      }
    },

    draw: function() {
      function requestFrame(callback) {
        var fn = window.C ||
                 window.webkitRequestAnimationFrame ||
                 window.mozRequestAnimationFrame ||
                 window.oRequestAnimationFrame ||
                 window.msRequestAnimationFrame;
        fn(callback);
      }

      var world = this;
      function doDraw() {
        var canvas = document.getElementById("main-canvas");
        canvas.width = canvas.offsetWidth;
        canvas.height = canvas.offsetHeight;
        world.update(new Date());
        world.render(canvas);
      }

      requestFrame(doDraw);
    }
  });

  var world = new World();
  $.ajax({
    url: "/api/v1/sectors?coords=0,0|-1,0|-1,-1|0,-1|1,0|1,1|0,1|-1,1|1,-1&gen=0",
    dataType: "json",
    success: function (data) {
      for (var i = 0; i < data.sectors.length; i++) {
        var sector = new Sector(data.sectors[i]);
        world.addObject(sector);
      }
      world.draw();
    },
    error: function() {
      alert("An error occurred fetching starfield data. Sorry.");
    }
  });

  // this is our click-and-drag code that handles scrolling around the world
  $(function() {
    var canvas = $("#main-canvas");
    var isDragging = false;
    var lastX = 0;
    var lastY = 0;
    canvas.on("mousedown", function(evnt) {
      isDragging = true;
      lastX = evnt.pageX;
      lastY = evnt.pageY;
    }).on("mouseup", function(evnt) {
      isDragging = false;
    }).on("mousemove", function(evnt) {
      if (isDragging) {
        var dx = evnt.pageX - lastX;
        var dy = evnt.pageY - lastY;
        lastX = evnt.pageX;
        lastY = evnt.pageY;

        world.drag(dx, dy);
      }
    });
  });
});
