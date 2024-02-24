import math, os
from mkdocs_macros import fix_url


def define_env(env):
    """
    This is the hook for defining variables, macros and filters

    - variables: the dictionary that contains the environment variables
    - macro: a decorator function, to declare a macro.
    - filter: a function with one of more arguments,
        used to perform a transformation
    """

    # add to the dictionary of variables available to markdown pages:
    env.variables['baz'] = "John Doe"

    # NOTE: you may also treat env.variables as a namespace,
    #       with the dot notation:
    env.variables.baz = "John Doe"

    #########################
    # macros
    env.variables['default'] = {'source_url': 'https://git.uclalemur.com/Suke/woodbot_source'}

    @env.macro
    def bar(x):
        return (2.3 * x) + 7

    # If you wish, you can  declare a macro with a different name:
    def f(x):
        return x * x

    env.macro(f, 'barbaz')

    # or to export some predefined function
    env.macro(math.floor)  # will be exported as 'floor'

    # create a jinja2 filter
    @env.filter
    def reverse(x):
        "Reverse a string (and uppercase)"
        return x.upper()[::-1]

    @env.macro
    def button(name, url, new_tab=True, button_color=None, *args, **kwargs):
        url = fix_url(url)
        button_str = "[%s](%s)" % (name, url)
        settings = '.md-button '
        if new_tab:
            settings += "target=_blank "

        if button_color is not None:
            settings += '.md-button--' + button_color + ' '

        button_str += '{ ' + settings + '}'
        return button_str

    @env.macro
    def link2lec(name, url):
        block = """
??? question "More About %s?"

    Do you want to review %s lecture? Click the link to jump. 
    %s
        """ % (name, name, button(name, url))
        return block

    @env.macro
    def leveled_hint(hint_url, level):
        url = fix_url(hint_url)

    @env.macro
    def get_repo_url():
        url = os.environ.get('CI_PROJECT_URL')
        return url

    @env.macro
    def embed(url, width="100%", height="1000px"):
        text = """<embed src = "%s" style = "width:%s; height: %s;" >""" % (url, width, height)
        return text

    @env.macro
    def submission_button(path_to_file, name=None):
        sub = env.variables['submission']

        domain = sub.get('domain')
        ide = sub.get('ide')
        # is first element always /builds?
        # is the second element always the user name?

        url = get_repo_url()
        # print(f"environment var {url}")
        # print(f"root_dir {env.variables.get('git').get('root_dir').split('/')}")

        user = env.variables.get('git').get('root_dir').split('/')[2]
        repo = sub.get('repo')
        tree = '/tree/'
        branch = sub.get('branch')
        path_to_file = '/-' + path_to_file

        link_list = [domain, ide, user, repo, tree, branch, path_to_file]
        link = ''
        for path_element in link_list:
            if path_element is None:
                raise f"path element is None"
            link += path_element

        if name is None:
            name = sub.get('default_button_text')
        return button(name, link, new_tab=True, )


