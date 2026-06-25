
img = Bitmap.new('test/rat')
img.save('test/rat.gif')

b = Bitmap.new(100, 100)

b.set_pixel(0, 0, Color.new(5, 105, 205, 200))
p(b.get_pixel(0, 0))

black = Color.new(0, 0, 0)
red = Color.new(255, 0, 0)
blue = Color.new(0, 0, 255)
yellow = Color.new(255, 255, 0)

b.fill_rect(b.rect, black)
b.gradient_fill_rect(2, 2, 96, 96, blue, yellow, true)
b.stretch_blt(Rect.new(10, 10, 80, 80), img, img.rect)
b.blur(5)

#b.draw_text(2, 2, 96, 96, "Hello")

b.save('test/a.png')
