// "Convert field to local variable in initializer section" "true-preview"
class TestInitializer {

    {
    field = true;
    System.out.println(field);
  }

}