
b = Bitmap.new(100, 100)
p([b.width, b.height])

b.set_pixel(0, 0, Color.new(5, 105, 205, 50))
p(b.get_pixel(0, 0))

black = Color.new(0, 0, 0)
red = Color.new(255, 0, 0)
blue = Color.new(0, 0, 255)
yellow = Color.new(255, 255, 0)

b.fill_rect(b.rect, black)
b.gradient_fill_rect(2, 2, 96, 96, blue, yellow, true)

b.draw_text(2, 2, 96, 96, "Hello")

b.save("test/a.png")
