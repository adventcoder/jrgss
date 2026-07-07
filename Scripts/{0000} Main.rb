
len = 0
loop do
    Graphics.update
    Input.update
    print("\r")
    print(' ' * len)
    s = Input.pressed.inspect
    print("\r")
    print(s)
    len = s.size
    if Input.trigger?(:C)
        if Input.press?(:SHIFT)
            raise 'oops!'
        else
            Graphics.resize_screen(640, 480)
        end
    end
    sleep(rand/60)
end
